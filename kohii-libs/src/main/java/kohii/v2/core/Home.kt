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

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.LifecycleOwner
import kohii.v2.common.Capsule
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.PlayableKey.Empty
import kohii.v2.internal.BindRequest
import kohii.v2.internal.HomeDispatcher
import kohii.v2.internal.RequestHandleImpl
import kohii.v2.internal.asString
import kohii.v2.internal.awaitStarted
import kohii.v2.internal.debugOnly
import kohii.v2.internal.hexCode
import kohii.v2.internal.logInfo
import kohii.v2.internal.logStackTrace
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlin.coroutines.cancellation.CancellationException

class Home private constructor(context: Context) {

  val application = context.applicationContext as Application

  internal val playables = mutableMapOf<Playable, PlayableKey>()
  internal val pendingRequests = mutableMapOf<Any /* Container, PlayableKey */, RequestHandleImpl>()
  private val scope = CoroutineScope(
    context = SupervisorJob() +
      Dispatchers.Main.immediate +
      CoroutineExceptionHandler { _, throwable -> debugOnly(throwable::printStackTrace) }
  )

  private val groups = mutableSetOf<Group>()
  private val dispatcher = HomeDispatcher(this)

  /* init {
    application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
      override fun onActivityStarted(activity: Activity) = Unit
      override fun onActivityResumed(activity: Activity) = Unit
      override fun onActivityPaused(activity: Activity) = Unit
      override fun onActivityStopped(activity: Activity) = Unit
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
      override fun onActivityDestroyed(activity: Activity) = Unit
    })
  } */

  internal fun registerManagerInternal(
    owner: Any,
    managerLifecycleOwner: LifecycleOwner,
    managerViewModel: Lazy<PlayableManager>,
  ): Manager {
    check(managerLifecycleOwner.lifecycle.currentState > DESTROYED) {
      "The manager $managerLifecycleOwner is already destroyed"
    }

    val groupLifecycleOwner = when (owner) {
      is ComponentActivity -> owner
      is Fragment -> owner.requireActivity()
      else -> managerLifecycleOwner
    }

    check(groupLifecycleOwner.lifecycle.currentState > DESTROYED) {
      "The group $groupLifecycleOwner is already destroyed"
    }

    val group: Group = groups
      .find { it.lifecycleOwner === groupLifecycleOwner }
      ?: Group(this, groupLifecycleOwner)
        .also { newGroup ->
          groups.add(newGroup)
          groupLifecycleOwner.lifecycle.addObserver(newGroup)
        }

    return group.managers
      .find { it.lifecycleOwner === managerLifecycleOwner && it.owner == owner }
      ?: Manager(
        home = this,
        owner = owner,
        group = group,
        playableManager = managerViewModel.value,
        lifecycleOwner = managerLifecycleOwner
      )
        .also { newManager ->
          group.addManager(newManager)
          managerLifecycleOwner.lifecycle.addObserver(newManager)
        }
  }

  internal fun startPlayable(playable: Playable) {
    playable.onReady()
    playable.onStart()
  }

  internal fun pausePlayable(playable: Playable) {
    playable.onPause()
  }

  internal fun cancelPlayableDestruction(playable: Playable) {
    "Home[${hexCode()}] cancels [PB=$playable] destruction".logStackTrace()
    dispatcher.cancelPlayableDestroy(playable)
  }

  internal fun destroyPlayableDelayed(
    playable: Playable,
    delayMillis: Long,
  ) {
    "Home[${hexCode()}] destroys [PB=$playable, delay=$delayMillis]".logStackTrace()
    cancelPlayableDestruction(playable)
    dispatcher.dispatchDestroyPlayable(playable, delayMillis)
  }

  internal fun enqueueRequest(
    container: Any,
    request: BindRequest,
  ): RequestHandle {
    // Cancel any existing Request for the same container and playable (by its key).
    pendingRequests.remove(container)?.cancel()
    pendingRequests.remove(request.playableKey)?.cancel()

    val deferredBindResult: Deferred<Result<Playback>> = scope.async {
      try {
        request.lifecycle.awaitStarted()
        val playback = request.onBind()
        request.callback?.onSuccess(playback, request.request)
        return@async Result.success(playback)
      } catch (error: Throwable) {
        if (error is CancellationException) {
          request.callback?.onCanceled(error, request.request)
        } else {
          request.callback?.onFailure(error, request.request)
        }
        return@async Result.failure(error)
      }
    }

    val requestHandle = RequestHandleImpl(
      request = request.request,
      home = this,
      lifecycle = request.lifecycle,
      deferred = deferredBindResult
    )

    // The request may complete immediately, also the Lifecycle is not destroyed.
    if (!requestHandle.isCompleted && request.lifecycle.currentState.isAtLeast(INITIALIZED)) {
      pendingRequests[container] = requestHandle
      if (request.playableKey != Empty) {
        pendingRequests[request.playableKey] = requestHandle
      }
    }
    "Home[${hexCode()}]_ENQUEUE_Request [handle=${requestHandle.hexCode()}] [R=$request] [C=${container.asString()}]".logInfo()
    return requestHandle
  }

  internal fun onGroupDestroyed(group: Group) {
    groups.remove(group)
    if (groups.isEmpty()) {
      // TODO: improve to avoid new allocation.
      pendingRequests.entries
        .toMutableSet()
        .onEach { (_, handle) -> handle.cancel() }
        .clear()
      pendingRequests.clear()
    }
  }

  //region Public APIs
  @ExperimentalKohiiApi
  fun cancel(tag: String) {
    playables.keys.firstOrNull { it.tag == tag }
      ?.playback
      ?.unbind()
      ?: pendingRequests.values.firstOrNull { it.request.tag == tag }?.cancel()
  }
  //endregion

  companion object {

    internal const val NO_TAG = ""

    private val capsule: Capsule<Home, Context> = Capsule(::Home)

    @JvmStatic
    operator fun get(context: Context): Home = capsule.get(context.applicationContext)

    @JvmStatic
    operator fun get(fragment: Fragment): Home = get(fragment.requireContext())
  }
}
