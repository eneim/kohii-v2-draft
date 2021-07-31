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

import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import kohii.v2.common.logInfo
import kohii.v2.core.PlayableState.Inactive
import kohii.v2.core.Playback.State.ACTIVATED
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.ATTACHED
import kohii.v2.core.Playback.State.CREATED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.internal.asString
import kohii.v2.internal.checkMainThread
import kohii.v2.internal.hexCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.util.ArrayDeque
import java.util.Timer
import java.util.TimerTask
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.concurrent.scheduleAtFixedRate

/**
 * An object that contains the information about the surface to play the media content.
 */
abstract class Playback(
  internal val playable: Playable,
  internal val bucket: Bucket,
  internal val manager: Manager,
  internal val container: Any,
  internal val tag: String,
) {

  // Note(eneim, 2021/04/30): Using ArrayDeque because it is fast and light-weight. It supports
  // iterating in both direction, which is nice. All the access to the callbacks are on the main
  // thread, so we do not need thread-safety. We do not need the ability to modify the callbacks
  // during iteration as well. While ArrayDeque is well-known as the best queue implementation, we
  // do not use it as a queue. But it is still a good choice for our use case.
  private val callbacks = ArrayDeque<Callback>(4 /* Skip internal size check */)

  /**
   * The current [Lifecycle.State] of the lifecycle that hosts this [Playback].
   */
  internal var lifecycleState: Lifecycle.State = manager.lifecycleOwner.lifecycle.currentState
    set(value) {
      "Playback[${hexCode()}]_LIFECYCLE [$field → $value]".logInfo()
      field = value
    }

  /**
   * Only used when the Playback is being removed. In that time, the Playable may be switched to
   * another Playback already.
   */
  internal val activePlayable: Playable? get() = playable.takeIf { it.playback === this }

  /**
   * Returns the current state of the Playback.
   */
  internal var state: State = CREATED
    @VisibleForTesting internal set(value) {
      "Playback[${hexCode()}]_STATE [$field → $value]".logInfo()
      field = value
      playbackStateFlow.value = value
    }

  abstract val token: Token

  /**
   * `true` if only the Lifecycle hosts this Playback is started.
   */
  open val isOnline: Boolean get() = lifecycleState.isAtLeast(STARTED)

  val isAdded: Boolean get() = state >= ADDED
  val isAttached: Boolean get() = state >= ATTACHED
  val isActive: Boolean get() = state >= ACTIVATED

  private val playbackStateFlow = MutableStateFlow(CREATED)
  private val playableActiveStateFlow = MutableStateFlow(false)
  private val playableStateMutableStateFlow = MutableStateFlow<PlayableState>(Inactive)

  private var playableStateJob: Job? = null
  private var stateUpdateTask: TimerTask? = null

  /**
   * A [Flow] that allows the client to collect the [PlayableState] of the underline [Playable].
   */
  val playableStateFlow = playableStateMutableStateFlow
    .onStart {
      playableStateJob?.cancel()
      playableStateJob = combine(playbackStateFlow, playableActiveStateFlow) { _, playableActive ->
        if (playableActive && isOnline && isActive) {
          stateUpdateTask?.cancel()
          stateUpdateTask = timer.scheduleAtFixedRate(delay = 0, period = FETCH_STATE_PERIOD_MS) {
            playableStateMutableStateFlow.value = activePlayable?.fetchPlayableState() ?: Inactive
          }
        } else {
          stateUpdateTask?.cancel()
        }
      }
        .launchIn(CoroutineScope(currentCoroutineContext()))
    }
    .onCompletion {
      playableStateJob?.cancel()
      stateUpdateTask?.cancel()
      playableStateJob = null
      stateUpdateTask = null
    }

  init {
    @Suppress("LeakingThis")
    "Playback[${hexCode()}]_CREATED".logInfo()
  }

  @MainThread
  internal open fun shouldActivate(): Boolean = false

  @MainThread
  internal open fun shouldPrepare(): Boolean = false

  @MainThread
  internal open fun shouldPlay(): Boolean = false

  override fun toString(): String {
    return "PK[${hexCode()}, s=$state, m=$manager, pb=PB@${playable.hexCode()}, t=$tag, rr=${playable.renderer?.asString()}]"
  }

  @MainThread
  internal fun addCallback(callback: Callback) {
    checkMainThread()
    callbacks.add(callback)
  }

  @Suppress("unused")
  @MainThread
  internal fun removeCallback(callback: Callback?) {
    checkMainThread()
    callbacks.remove(callback)
  }

  /**
   * Called by the [Manager] to perform adding this Playback. This method will call [onAdd], and its
   * state will be changed: [CREATED]->[onAdd]->[ADDED].
   */
  @MainThread
  internal fun performAdd() {
    checkMainThread()
    checkState(CREATED)
    onAdd()
    state = ADDED
    for (callback in callbacks) {
      callback.onAdded(this)
    }
  }

  /**
   * Called by the [Manager] to perform starting this Playback.
   *
   * This method will call [onAttach], its state will be changed: [ADDED]->[onAttach]->[ATTACHED].
   */
  @MainThread
  internal fun performAttach() {
    checkMainThread()
    checkState(ADDED)
    onAttach()
    state = ATTACHED
    for (callback in callbacks) {
      callback.onAttached(this)
    }
  }

  /**
   * Called by the [Manager] to perform resuming this Playback.
   *
   * This method will call [onActivate], its state will be changed:
   * [ATTACHED]->[onActivate]->[ACTIVATED].
   */
  @MainThread
  internal fun performActivate() {
    checkMainThread()
    checkState(ATTACHED)
    onActivate()
    state = ACTIVATED
    for (callback in callbacks) {
      callback.onActivated(this)
    }
  }

  /**
   * Called by the [Manager] to perform pausing this Playback.
   *
   * This method will call [onDeactivate], its state will be changed:
   * [ACTIVATED]->[ATTACHED]->[onDeactivate].
   */
  @MainThread
  internal fun performDeactivate() {
    checkMainThread()
    checkState(ACTIVATED)
    state = ATTACHED
    for (callback in callbacks) {
      callback.onDeactivated(this)
    }
    onDeactivate()
  }

  /**
   * Called by the [Manager] to perform stopping this Playback.
   *
   * This method will call [onDetach], its state will be changed: [ATTACHED]->[ADDED]->[onDetach].
   */
  @MainThread
  internal fun performDetach() {
    checkMainThread()
    checkState(ATTACHED)
    state = ADDED
    for (callback in callbacks) {
      callback.onDetached(this)
    }
    onDetach()
  }

  /**
   * Called by the [Manager] to perform removing this Playback.
   *
   * This method will call [onRemove], its state will be changed: [ADDED]->[REMOVED]->[onRemove].
   */
  @MainThread
  internal fun performRemove() {
    checkMainThread()
    checkState(ADDED)
    state = REMOVED
    for (callback in callbacks) {
      callback.onRemoved(this)
    }
    onRemove()
  }

  /**
   * Called after the Playback is attached, to make sure if it is activated or not.
   */
  @MainThread
  internal fun maybeActivated(): Boolean {
    checkMainThread()
    onRefresh()
    return shouldActivate()
  }

  //region Lifecycle callbacks
  @MainThread
  @CallSuper
  protected open fun onAdd(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onRemove(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onAttach(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onDetach(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onActivate(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onDeactivate() {
    detachRenderer()
  }
  //endregion

  /**
   * Called by the [playable] to notify this [Playback] that the media playback is started.
   */
  @CallSuper
  internal open fun onStarted() {
    "Playback[${hexCode()}]_STARTED".logInfo()
  }

  /**
   * Called by the [playable] to notify this [Playback] that the media playback is paused.
   */
  @CallSuper
  internal open fun onPaused() {
    "Playback[${hexCode()}]_PAUSED".logInfo()
  }

  /**
   * Called by [Group] to refresh the Playback internal.
   */
  @CallSuper
  internal open fun onRefresh() {
    "Playback[${hexCode()}]_REFRESH".logInfo()
  }

  /**
   * Called by the [playable] to notify this class that it is available for this class to use.
   */
  internal fun onPlayableActiveStateChanged(active: Boolean) {
    playableActiveStateFlow.value = active
  }

  /**
   * Force this class to detach the current renderer.
   */
  protected abstract fun detachRenderer()

  /**
   * See also [androidx.lifecycle.Lifecycle.State]
   */
  enum class State {
    /**
     * Removed state for a Playback. After this state is reached, the Playback is no longer usable.
     * This state is reached right before a call to [onRemove].
     */
    REMOVED,

    /**
     * Created state for a Playback. This is the state when a Playback is initialized but is not
     * added to its Manager.
     */
    CREATED,

    /**
     * Added state for a Playback. This is the state when a Playback is added to its Manager. This
     * state is reached in two cases:
     * - Right after a call to [onAdd].
     * - Right before a call to [onDetach].
     */
    ADDED,

    /**
     * Started state for a Playback. This is the state when the container of a Playback is attached
     * to its parent. This state is reached in two cases:
     * - Right after a call to [onAttach].
     * - Right before a call to [onDeactivate].
     */
    ATTACHED,

    /**
     * Resumed state for a Playback. This is the state when the container of a Playback is attached,
     * and visible enough so that it can be playing at any time. This state is reached right after
     * [onActivate] is called.
     */
    ACTIVATED,
  }

  abstract class Token

  /**
   * Class that can receive the changes of a Playback lifecycle via callback methods.
   */
  interface Callback {

    /**
     * Called when the [playback] is added to the manager. [Playback.state] is [ADDED].
     */
    @MainThread
    fun onAdded(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is remove from the manager. [Playback.state] is [REMOVED].
     */
    @MainThread
    fun onRemoved(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is started. If the [Playback.container] is a [View], this event is
     * the same as the [View.onAttachedToWindow] event. [Playback.state] is [ATTACHED].
     */
    @MainThread
    fun onAttached(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is stopped. If the [Playback.container] is a [View], this event is
     * the same as the [View.onDetachedFromWindow] event. [Playback.state] is [ADDED].
     */
    @MainThread
    fun onDetached(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is resumed. If the [Playback.container] is a [View], this event is
     * the same as when the container has at least one pixel on the screen.
     */
    @MainThread
    fun onActivated(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is paused. If the [Playback.container] is a [View], this event is
     * the same as when the container doesn't meed the "resume" condition.
     */
    @MainThread
    fun onDeactivated(playback: Playback): Unit = Unit
  }

  class Config(
    val trigger: Float = 0.65f,
    var callback: Callback? = null
  ) {
    fun withCallback(callback: Callback) = apply { this.callback = callback }
  }

  companion object {
    private const val FETCH_STATE_PERIOD_MS = 200L
    private val timer: Timer by lazy(NONE, ::Timer)

    @Throws(IllegalStateException::class)
    private fun Playback.checkState(expected: State): Unit =
      check(state == expected) { "Expected Playback state: $expected, Actual state: $state" }
  }
}
