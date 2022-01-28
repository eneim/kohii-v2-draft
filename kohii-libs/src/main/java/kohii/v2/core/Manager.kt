/*
 * Copyright (c) 2021. Nam Nguyen, nam@ene.im
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
import kohii.v2.internal.BindRequest
import kohii.v2.internal.DynamicViewPlaybackCreator
import kohii.v2.internal.StaticViewPlaybackCreator
import kohii.v2.internal.checkMainThread
import kohii.v2.internal.debugOnly
import kohii.v2.internal.hexCode
import kohii.v2.internal.logDebug
import kohii.v2.internal.logError
import kohii.v2.internal.logInfo
import kohii.v2.internal.onRemoveEach
import kohii.v2.internal.partitionToMutableSets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.ArrayDeque

/**
 * A class that represents a [Fragment] or an [Activity], and has the capability to manage multiple
 * [Playback]s.
 *
 * TODO(eneim): this class should be named "PlaybackManager".
 */
class Manager(
  internal val home: Home,
  internal val owner: Any,
  internal val group: Group,
  internal val playableManager: PlayableManager,
  internal val lifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver, LifecycleEventObserver, RendererProviderManager, PlaybackManager {

  @VisibleForTesting
  internal var stickyBucket: Bucket? = null

  private val buckets = ArrayDeque<Bucket>(4 /* Skip size check */)
  private val playbacks = linkedMapOf<Any /* Container */, Playback>()
  private val playbacksFlow = MutableStateFlow<List<Playback>>(emptyList())

  @Suppress("RemoveExplicitTypeArguments")
  private val playbackCreators = setOf<PlaybackCreator>(
    StaticViewPlaybackCreator(manager = this),
    DynamicViewPlaybackCreator(manager = this, rendererProviderManager = this)
  )

  // Last added item is the first to be checked in `getRendererProvider`.
  private val rendererProviders = ArrayDeque<RendererProvider>()

  private var internalLock: Boolean = false

  // TODO(eneim): support external lock.
  private val isLocked: Boolean get() = internalLock
  private val isStarted: Boolean get() = lifecycleOwner.lifecycle.currentState.isAtLeast(STARTED)

  internal val isChangingConfigurations: Boolean
    get() = when (owner) {
      is Activity -> owner.isChangingConfigurations
      is Fragment -> owner.activity?.isChangingConfigurations == true
      else -> false
    }

  override fun toString(): String = "M@${hexCode()}"

  internal fun refresh(): Unit = group.onRefresh()

  internal fun findPlaybackForContainer(container: Any): Playback? = playbacks[container]

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
      stickyBucket?.let(::unstickBucketInternal)
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
    refresh()
  }

  internal fun notifyPlaybackAdded(playback: Playback) {
    val currentPlaybacks = playbacksFlow.value
    playbacksFlow.value = currentPlaybacks + playback
  }

  /**
   * If this method is called during a binding in which a [Playable] is switched from a [Playback] to
   * another, it will be called before the [Playable] is bound to the new [Playback].
   *
   * @param clearPlayable If `true`, also clear the current [Playable.playback] value. This flag is
   * `false` only when there is an upcoming binding of the [Playable] from [playback] to another
   * [Playback].
   *
   * @see [BindRequest.onBind]
   */
  @MainThread
  internal fun removePlayback(
    playback: Playback,
    clearPlayable: Boolean = true,
  ) {
    "Manager[${hexCode()}]_REMOVE_Playback_Begin [PK=$playback]".logDebug()
    checkMainThread()
    val container = playback.container
    val removedPlayback = playbacks.remove(container)
    debugOnly {
      require(removedPlayback === playback) {
        "Removing $playback for container $container, but got a different one $removedPlayback"
      }
    }
    onRemovePlayback(playback, clearPlayable)
    refresh()
    "Manager[${hexCode()}]_REMOVE_Playback_End [PK=$playback]".logDebug()
  }

  internal fun notifyPlaybackRemoved(playback: Playback) {
    val currentPlaybacks = playbacksFlow.value
    playbacksFlow.value = currentPlaybacks - playback
  }

  private fun onRemovePlayback(
    playback: Playback,
    clearPlayable: Boolean = true,
  ) {
    playback.isRemoving = true
    if (playback.isAttached) {
      if (playback.isActive) playback.performDeactivate()
      playback.performDetach()
    }
    playback.bucket.removeContainer(playback.container)
    // Playback should not let the bucket touch its container at this point.
    playback.performRemove()
    if (clearPlayable) {
      playback.playable.playback = null
    }
  }

  @MainThread
  internal fun requirePlaybackCreator(
    playable: Playable,
    container: Any,
  ): PlaybackCreator {
    return playbackCreators.firstOrNull { creator ->
      creator.accept(playable = playable, container = container)
    } ?: throw IllegalStateException("No PlaybackCreator available for $playable and $container.")
  }

  @Throws(IllegalArgumentException::class)
  @MainThread
  internal fun requireBucket(container: Any): Bucket {
    // Check from the top of the stack.
    val bucket = buckets.lastOrNull { it.accept(container) }
    if (bucket == null) {
      val error = IllegalArgumentException("No bucket in $this accepts container $container.")
      "Manager[${hexCode()}]_BIND_End error=$error".logError()
      throw error
    }

    return bucket
  }

  @MainThread
  internal fun splitPlaybacks(): Pair<Collection<Playback> /* toPlay */, Collection<Playback> /* toPause */> {
    if (playbacks.isEmpty()) return emptySet<Playback>() to emptySet()

    val (activePlaybacks, inactivePlaybacks) = refreshPlaybackStates()
    return if (isLocked || !isStarted) {
      emptySet<Playback>() to playbacks.values
    } else {
      val toPlay = arraySetOf<Playback>()
      activePlaybacks
        .groupBy(Playback::bucket)
        .map { (bucket, playbacks) ->
          if (playbacks.isEmpty()) return@map emptyList()
          val candidates = playbacks.filter(bucket::allowToPlay)
          bucket.selectToPlay(candidates)
        }
        .firstOrNull(Collection<Playback>::isNotEmpty)
        .orEmpty()
        .also { playbacks ->
          toPlay.addAll(playbacks)
          activePlaybacks.removeAll(playbacks)
        }

      val toPause = activePlaybacks.apply { addAll(inactivePlaybacks) }
      toPlay to toPause
    }
  }

  // Returns a Pair of [a set of Activated playbacks] to [a set of Deactivated playbacks].
  private fun refreshPlaybackStates(): Pair<MutableSet<Playback>, MutableSet<Playback>> {
    return playbacks.values
      .onEach { playback ->
        playback.performRefresh()
        if (!playback.isActive && playback.shouldActivate()) {
          if (!playback.isAttached) playback.performAttach()
          playback.performActivate()
        } else if (playback.isActive && !playback.shouldActivate()) {
          playback.performDeactivate()
        }
      }
      .filter(predicate = Playback::isAttached)
      .partitionToMutableSets(predicate = Playback::isActive)
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
    debugOnly {
      val playback = playbacks[container]
      check(playback != null) {
        "Container $container is managed, but no corresponding playback are found."
      }
    }
    refresh()
  }

  // Note(eneim, 2021/04/30): removing a Playback may trigger Bucket.removeContainer, which calls
  // this method again. So we allow the cached Playback to be null.
  // Note(eneim, 2021/10/10): removing a Container should also remove the Playback if it is still
  // there.
  @MainThread
  internal fun onContainerRemoved(container: Any) {
    checkMainThread()
    playbacks[container]?.activePlayable?.playback = null
  }
  //endregion

  override fun onStateChanged(
    source: LifecycleOwner,
    event: Event,
  ) {
    "Manager[${hexCode()}]_Lifecycle [State -> ${event.targetState}]".logInfo()
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
    playbacks.values.onRemoveEach(::onRemovePlayback)

    val lifecycle = owner.lifecycle
    rendererProviders.onEach { rendererProvider ->
      rendererProvider.onDestroy(owner)
      lifecycle.removeObserver(rendererProvider)
    }
      .clear()

    buckets.onRemoveEach(Bucket::onRemove)
    home.pendingRequests.values.removeAll { handle -> handle.lifecycle === lifecycle }
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

  override fun getRendererProvider(playback: Playback): RendererProvider {
    return rendererProviders
      .firstOrNull { provider -> provider.accept(playback) }
      ?: throw IllegalArgumentException("No RendererProvider found for $playback.")
  }
  //endregion

  //region Public APIs
  /**
   * Returns a [Bucket] whose [Bucket.root] is the provided [root].
   *
   * This method must also add the [Bucket] to its cache.
   */
  fun bucket(root: Any): Bucket = buckets.find { it.root === root }
    ?: Bucket[this, root].also(::addBucketInternal)

  /**
   * Returns a [Flow] of [Playback] that can be used to know when a [Playback] for a specific tag
   * is available/added.
   */
  fun getPlaybackFlow(tag: String): Flow<Playback?> = playbacksFlow
    .map { playbacks ->
      playbacks.find { playback -> playback.tag == tag }
    }
    .distinctUntilChanged()
  //endregion

  internal companion object {
    const val DEFAULT_DESTRUCTION_DELAY_MS = 800L // ProcessLifecycle delay + 100ms
  }
}
