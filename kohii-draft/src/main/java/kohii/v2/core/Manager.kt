/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii.v2.core

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.collection.arraySetOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v2.common.logDebug
import kohii.v2.common.logInfo
import kohii.v2.common.partitionToMutableSets
import kohii.v2.internal.BindRequest
import kohii.v2.internal.DynamicViewPlaybackCreator
import kohii.v2.internal.StaticViewPlaybackCreator
import kohii.v2.internal.checkMainThread
import kohii.v2.internal.hexCode
import kohii.v2.internal.onNotNull
import java.util.ArrayDeque

private typealias MutablePlaybackSet = MutableSet<Playback>

/**
 * A class that represents a [Fragment] or an [Activity], and has the capability to manage multiple
 * [Playback]s.
 *
 * TODO(eneim): this class should be named "PlaybackManager".
 */
class Manager(
  @PublishedApi
  internal val home: Home,
  internal val group: Group,
  internal val playableManager: PlayableManager,
  internal val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver, LifecycleEventObserver, RendererProviderManager, PlaybackManager {

  @VisibleForTesting
  internal var stickyBucket: Bucket? = null

  @VisibleForTesting
  internal val buckets = ArrayDeque<Bucket>(4 /* Skip size check */)
  internal val playbacks = linkedMapOf<Any /* Container type */, Playback>()

  // Last added item is the first to be checked in `getRendererProvider`.
  private val rendererProviders = ArrayDeque<RendererProvider>()
  private val playbackCreators = mutableSetOf<PlaybackCreator>()

  private var internalLock: Boolean = false

  // TODO(eneim): support external lock.
  private val isLocked: Boolean get() = internalLock
  private val isStarted: Boolean get() = lifecycleOwner.lifecycle.currentState.isAtLeast(STARTED)

  init {
    // Register default `PlaybackCreator`s
    playbackCreators.add(StaticViewPlaybackCreator())
    playbackCreators.add(DynamicViewPlaybackCreator(rendererProviderManager = this))

    // Register default `RendererProvider`s
    rendererProviders.add(PlayerViewRendererProvider())
  }

  override fun toString(): String = "M@${hexCode()}"

  internal fun refresh(): Unit = group.onRefresh()

  //region Bucket APIs
  @MainThread
  internal fun addBucketInternal(bucket: Bucket) {
    checkMainThread()
    if (!buckets.contains(bucket)) {
      buckets.add(bucket)
      bucket.onAdd()
      refresh()
    }
  }

  @MainThread
  internal fun removeBucketInternal(bucket: Bucket) {
    checkMainThread()
    if (buckets.remove(bucket)) {
      bucket.onRemove()
      refresh()
    }
  }

  @MainThread
  internal fun stickBucketInternal(bucket: Bucket) {
    checkMainThread()
    require(buckets.contains(bucket)) {
      "$bucket is not registered. Please add it before stick it."
    }
    if (bucket !== stickyBucket) {
      stickyBucket.onNotNull(::unstickBucketInternal)
      stickyBucket = bucket
      buckets.push(bucket)
      refresh()
    }
  }

  @MainThread
  internal fun unstickBucketInternal(bucket: Bucket) {
    checkMainThread()
    if (stickyBucket === bucket && bucket === buckets.peekFirst()) {
      buckets.removeFirst()
      stickyBucket = null
      refresh()
    }
  }
  //endregion

  //region Playback APIs
  @Suppress("unused")
  @MainThread
  internal fun addPlayback(playback: Playback) {
    "Manager[${hexCode()}]_ADD_Playback [PK=$playback]".logInfo()
    checkMainThread()
    val container = playback.container
    val removedPlayback: Playback? = playbacks.put(container, playback)
    // No existing Playback are allowed.
    require(removedPlayback == null) {
      """
        Adding PK[${playback.hexCode()}, ${playback.state}] for container C@${container.hashCode()}, 
        but found existing one: PK[${removedPlayback?.hexCode()}, ${removedPlayback?.state}].
      """.trimIndent()
    }
    // Playback should not let the bucket touch its container at this point.
    playback.performAdd()
    playback.bucket.addContainer(container)
    playableManager.addPlayable(playback.playable)
    refresh()
  }

  /**
   * @param clearPlayable If `true`, also clear the current [Playable.playback] value. This flag is
   * used only when there is an upcoming binding of the [Playable] to another [Playback].
   *
   * @see [BindRequest.onBind]
   */
  @MainThread
  internal fun removePlayback(playback: Playback, clearPlayable: Boolean = true) {
    "Manager[${hexCode()}]_REMOVE_Playback_Begin [PK=$playback]".logDebug()
    checkMainThread()
    val container = playback.container
    val removedPlayback = playbacks.remove(container)
    require(removedPlayback === playback) {
      "Removing $playback for container $container, but got a different one $removedPlayback"
    }
    if (playback.isAttached) {
      if (playback.isActive) playback.performDeactivate()
      playback.performDetach()
    }
    playback.bucket.removeContainer(container)
    // Playback should not let the bucket touch its container at this point.
    playback.performRemove()
    if (clearPlayable) {
      playback.activePlayable?.internalPlayback = null
    }
    refresh()
    "Manager[${hexCode()}]_REMOVE_Playback_End [PK=$playback]".logDebug()
  }

  @MainThread
  internal fun requirePlaybackCreator(playable: Playable, container: Any): PlaybackCreator {
    return playbackCreators
      .firstOrNull { creator -> creator.accepts(playable = playable, container = container) }
      ?: throw IllegalStateException("No PlaybackCreator available for $playable and $container.")
  }

  @MainThread
  internal fun splitPlaybacks(): Pair<Set<Playback> /* toPlay */, Set<Playback> /* toPause */> {
    val (activePlaybacks, inactivePlaybacks) = refreshPlaybackStates()
    return if (isLocked || !isStarted) {
      emptySet<Playback>() to (activePlaybacks + inactivePlaybacks)
    } else {
      val toPlay = arraySetOf<Playback>()
      playbacks.values
        .groupBy(Playback::bucket)
        // TODO(eneim): complete the selection logic, respect the sticky Bucket, etc.
        .map { (bucket, playbacks) ->
          if (playbacks.isEmpty()) return@map emptyList()
          val candidates = playbacks.filter(bucket::allowToPlay)
          listOfNotNull(candidates.firstOrNull())
        }
        .firstOrNull(List<Playback>::isNotEmpty)
        ?.also { playbacks ->
          toPlay.addAll(playbacks)
          activePlaybacks.removeAll(playbacks)
        }

      val toPause = activePlaybacks.apply { addAll(inactivePlaybacks) }
      toPlay to toPause
    }
  }

  private fun refreshPlaybackStates(): Pair<MutablePlaybackSet /* Active */, MutablePlaybackSet /* InActive */> {
    val toActive = playbacks.filterValues { !it.isActive && it.shouldActivate() }
      .values
    val toInActive = playbacks.filterValues { it.isActive && !it.shouldActivate() }
      .values

    toActive.forEach(Playback::performActivate)
    toInActive.forEach(Playback::performDeactivate)

    return playbacks.values
      .filter(Playback::isAttached)
      .partitionToMutableSets(
        predicate = Playback::isActive,
        transform = { playback -> playback }
      )
  }
  //endregion

  //region Container APIs
  @MainThread
  internal fun onContainerAttached(container: Any) {
    checkMainThread()
    val playback = playbacks[container]
    checkNotNull(playback) {
      "Container $container is managed, but no corresponding playback are found."
    }
    check(playback.isAdded) { "Playback $playback is not added." }
    playback.performAttach()
    if (playback.maybeActivated()) playback.performActivate()
    refresh()
  }

  @MainThread
  internal fun onContainerDetached(container: Any) {
    checkMainThread()
    val playback = playbacks[container]
    checkNotNull(playback) {
      "Container $container is managed, but no corresponding playback are found."
    }
    if (playback.isAttached) {
      if (playback.isActive) playback.performDeactivate()
      playback.performDetach()
    }
    refresh()
  }

  @MainThread
  internal fun onContainerUpdated(container: Any) {
    checkMainThread()
    val playback = playbacks[container]
    check(playback != null) {
      "Container $container is managed, but no corresponding playback are found."
    }
    refresh()
  }

  // Note(eneim, 2021/04/30): removing a Playback may trigger Bucket.removeContainer, which calls
  // this method again. So we allow the cached Playback to be null.
  @MainThread
  internal fun onContainerRemoved(container: Any) {
    checkMainThread()
    playbacks[container]?.activePlayable?.internalPlayback = null
  }
  //endregion

  override fun onStateChanged(source: LifecycleOwner, event: Event) {
    playbacks.forEach { (_, playback) -> playback.lifecycleState = event.targetState }
  }

  override fun onStart(owner: LifecycleOwner) {
    internalLock = false
    refresh()
  }

  override fun onStop(owner: LifecycleOwner) {
    internalLock = true
    buckets.forEach(Bucket::onStop)
    refresh()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    playbacks.values.toMutableList()
      .onEach(::removePlayback)
      .clear()
    playbacks.clear()

    val lifecycle = lifecycleOwner.lifecycle
    rendererProviders.onEach { rendererProvider ->
      rendererProvider.onDestroy(lifecycleOwner)
      lifecycle.removeObserver(rendererProvider)
    }
      .clear()

    buckets.removeAll { bucket ->
      bucket.onRemove()
      true
    }
    group.removeManager(this)
  }

  //region RendererProviderManager
  override fun addRendererProvider(rendererProvider: RendererProvider) {
    if (!rendererProviders.contains(rendererProvider)) {
      rendererProviders.addFirst(rendererProvider)
      lifecycleOwner.lifecycle.addObserver(rendererProvider)
    }
  }

  override fun removeRendererProvider(rendererProvider: RendererProvider) {
    if (rendererProviders.remove(rendererProvider)) {
      rendererProvider.onDestroy(lifecycleOwner)
      lifecycleOwner.lifecycle.removeObserver(rendererProvider)
    }
  }

  override fun getRendererProvider(playable: Playable): RendererProvider {
    return rendererProviders.firstOrNull { provider ->
      provider.accepts(playable)
    } ?: throw IllegalArgumentException("No RendererProvider found for $playable.")
  }
  //endregion

  /**
   * Returns a [Bucket] whose [Bucket.root] is the provided [root].
   *
   * This method must also add the [Bucket] to its cache.
   */
  fun bucket(root: Any): Bucket = buckets.find { it.root === root }
    ?: Bucket[this, root].also(::addBucketInternal)
}
