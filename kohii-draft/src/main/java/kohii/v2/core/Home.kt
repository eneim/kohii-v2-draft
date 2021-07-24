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

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.MediaItem
import kohii.v2.common.Capsule
import kohii.v2.common.debugOnly
import kohii.v2.common.logInfo
import kohii.v2.internal.BindRequest
import kohii.v2.internal.HomeDispatcher
import kohii.v2.internal.ManagerViewModel
import kohii.v2.internal.PlayerViewPlayableCreator
import kohii.v2.internal.asString
import kohii.v2.internal.awaitStarted
import kohii.v2.internal.hexCode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class Home private constructor(context: Context) {

  internal val playables = mutableMapOf<Playable, Any>()
  private val requests = mutableMapOf<Any, BindRequest>()

  private val dispatcher = HomeDispatcher(this, Looper.getMainLooper())
  private val groups = mutableSetOf<Group>()
  private val playableCreators = mutableListOf<Pair<PlayableCreator, Class<*>>>()

  private val scope = CoroutineScope(
    SupervisorJob() +
        Dispatchers.Main +
        CoroutineExceptionHandler { _, throwable -> debugOnly(throwable::printStackTrace) }
  )

  init {
    // Register default `PlayableCreator`s
    val defaultPlayerViewPlaybackCreator = PlayerViewPlayableCreator()
    playableCreators.add(Pair(defaultPlayerViewPlaybackCreator, MediaItem::class.java))
    playableCreators.add(Pair(defaultPlayerViewPlaybackCreator, List::class.java))
  }

  private fun registerManagerInternal(
    groupLifecycleOwner: LifecycleOwner,
    managerLifecycleOwner: LifecycleOwner,
    managerViewModel: ManagerViewModel,
  ): Manager {
    val groupState = groupLifecycleOwner.lifecycle.currentState
    check(groupState > DESTROYED) {
      "The group lifecycle [$groupLifecycleOwner](state=$groupState) is already destroyed"
    }
    val managerState = managerLifecycleOwner.lifecycle.currentState
    check(managerState > DESTROYED) {
      "The manager lifecycle [$managerLifecycleOwner](state=$managerState) is already destroyed"
    }

    val group: Group = groups
      .find { it.lifecycleOwner === groupLifecycleOwner } ?: Group(this, groupLifecycleOwner)
      .also { newGroup ->
        groups.add(newGroup)
        groupLifecycleOwner.lifecycle.addObserver(newGroup)
      }

    return group.managers.find { it.lifecycleOwner === managerLifecycleOwner }
      ?: Manager(
        home = this,
        group = group,
        playableManager = managerViewModel,
        lifecycleOwner = managerLifecycleOwner
      )
        .also { newManager ->
          group.addManager(newManager)
          managerLifecycleOwner.lifecycle.addObserver(newManager)
        }
  }

  internal fun startPlayable(playable: Playable) {
    playable.onStart()
  }

  internal fun pausePlayable(playable: Playable) {
    playable.onPause()
  }

  // TODO(eneim): re-evaluate the necessary of this method.
  internal fun keepPlayable(playable: Playable) {
    dispatcher.cancelPlayableRelease(playable)
    dispatcher.cancelPlayableDestroy(playable)
  }

  internal fun releasePlayable(playable: Playable) {
    dispatcher.dispatchReleasePlayable(playable)
  }

  internal fun destroyPlayable(playable: Playable) {
    dispatcher.dispatchDestroyPlayable(playable)
  }

  internal fun enqueueRequest(container: Any, request: BindRequest): RequestHandle {
    "Home[${hexCode()}]_ENQUEUE_Request [R=$request] [C=${container.asString()}]".logInfo()
    keepPlayable(request.playable)
    requests.put(container, request)?.cancel()
    val deferredBindResult: Deferred<Result<Playback>> = scope.async {
      bindRequestOnMain(container)
    }

    deferredBindResult.invokeOnCompletion { error ->
      error?.printStackTrace()
    }

    return BaseRequestHandle(deferredBindResult)
  }

  private suspend fun bindRequestOnMain(container: Any): Result<Playback> {
    val request = requests.remove(container)
      ?: return Result.failure(IllegalStateException("Request not found."))
    request.manager.lifecycleOwner.lifecycle.awaitStarted()
    return request.onBind()
  }

  internal fun requirePlayableCreator(data: Any): PlayableCreator {
    return playableCreators.firstOrNull { (playableCreator, mediaType) ->
      mediaType.isAssignableFrom(data::class.java) &&
          playableCreator.accepts(data)
    }?.first ?: throw IllegalStateException("No PlayableCreator available for $data.")
  }

  internal fun onGroupDestroyed(group: Group) {
    groups.remove(group)
  }

  internal fun shutdown() {
    playableCreators
      .onEach { (creator, _) -> creator.cleanUp() }
      .clear()
  }

  //region Public APIs
  fun register(activity: ComponentActivity): Manager {
    val managerViewModel: ManagerViewModel by activity.viewModels()
    return registerManagerInternal(
      groupLifecycleOwner = activity,
      managerLifecycleOwner = activity,
      managerViewModel = managerViewModel
    )
  }

  fun register(fragment: Fragment): Manager {
    val managerViewModel: ManagerViewModel by fragment.viewModels()
    return registerManagerInternal(
      groupLifecycleOwner = fragment.requireActivity(),
      managerLifecycleOwner = fragment.viewLifecycleOwner,
      managerViewModel = managerViewModel
    )
  }

  /**
   * Prepare the [Request.Builder] for a mediaItem that can be used to bind the playable content to
   * a container.
   *
   * @see [Request.bind].
   */
  inline fun setUp(
    data: Any,
    crossinline options: Request.Builder.() -> Unit = {}
  ): Request.Builder = Request.Builder(data).apply(options)

  //endregion

  companion object {

    internal val NO_TAG = object : Any() {
      override fun toString(): String = "NO_TAG"
    }

    private val capsule: Capsule<Home, Context> = Capsule(::Home)

    @JvmStatic
    operator fun get(context: Context): Home = capsule.get(context.applicationContext)
  }
}
