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

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.collection.arraySetOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Playable.Controller
import kohii.v2.core.Playback.State.ACTIVATED
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.ATTACHED
import kohii.v2.core.Playback.State.CREATED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.internal.PlayableControllerImpl
import kohii.v2.internal.asString
import kohii.v2.internal.checkMainThread
import kohii.v2.internal.hexCode
import kohii.v2.internal.logInfo
import java.util.ArrayDeque
import kotlin.LazyThreadSafetyMode.NONE

/**
 * An object that contains the information about the surface to play the media content.
 */
// TODO: allow the Playback to configure the renderer when it is available.
abstract class Playback(
  internal val playable: Playable,
  internal val bucket: Bucket,
  internal val manager: Manager,
  val container: Any,
  internal val tag: String,
  internal val config: Config,
) {

  // Note(eneim, 2021/04/30): Using ArrayDeque because it is fast and light-weight. It supports
  // iterating in both direction, which is nice. All the access to the callbacks are on the main
  // thread, so we do not need thread-safety. We do not need the ability to modify the callbacks
  // during iteration as well. While ArrayDeque is well-known as the best queue implementation, we
  // do not use it as a queue. But it is still a good choice for our use case.
  private val eventListeners = ArrayDeque(config.playbackEventListeners)
  private val playerEventListeners = ArrayDeque(config.playerEventListeners)

  internal val trigger: Float = config.trigger

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
    private set(value) {
      "Playback[${hexCode()}]_STATE [$field → $value]".logInfo()
      val oldState = field
      if (field != value) {
        field = value
        for (eventListener in eventListeners) {
          eventListener.onStateChanged(playback = this, fromState = oldState, toState = value)
        }
      }
    }

  // This field is set to true once when this Playback is being removed.
  internal var isRemoving: Boolean = false

  /**
   * A [Controller] that can be used to manually start or pause a [Playable].
   */
  val controller: Controller by lazy(NONE) {
    PlayableControllerImpl(this)
  }

  val isStarted: Boolean get() = activePlayable?.isStarted == true

  val isPlaying: Boolean get() = activePlayable?.isPlaying == true

  open val isOnline: Boolean get() = lifecycleState.isAtLeast(STARTED)

  val isAdded: Boolean get() = state >= ADDED
  val isAttached: Boolean get() = state >= ATTACHED
  val isActive: Boolean get() = state >= ACTIVATED

  abstract val token: Token

  @RestrictTo(LIBRARY_GROUP_PREFIX)
  internal val rawListener: Player.Listener = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
      val playback = this@Playback
      for (callback in playerEventListeners) {
        callback.onStateChanged(playback = playback, state = playbackState)
      }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
      val playback = this@Playback
      for (callback in playerEventListeners) {
        callback.onVideoSizeChanged(playback = playback, videoSize = videoSize)
      }
    }
  }

  /**
   * This value should be set once.
   */
  @ExperimentalKohiiApi
  internal var chain: Chain? = null
    internal set(value) {
      check(field == null) { "This value is already set for this Playback." }
      field = value
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
  internal fun addPlaybackEventListener(listener: PlaybackEventListener) {
    checkMainThread()
    if (!eventListeners.contains(listener)) {
      eventListeners.add(listener)
    }
  }

  @Suppress("unused")
  @MainThread
  internal fun removePlaybackEventListener(listener: PlaybackEventListener?) {
    checkMainThread()
    eventListeners.remove(listener)
  }

  @MainThread
  internal fun addPlayerEventListener(listener: PlayerEventListener) {
    checkMainThread()
    if (!playerEventListeners.contains(listener)) {
      playerEventListeners.addFirst(listener)
    }
  }

  @MainThread
  internal fun removePlayerEventListener(listener: PlayerEventListener?) {
    checkMainThread()
    playerEventListeners.remove(listener)
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
    manager.notifyPlaybackAdded(this)
    for (listener in eventListeners) {
      listener.onAdded(this)
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
    for (callback in eventListeners) {
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
    activePlayable?.onPrepare(config.preload)
    state = ACTIVATED
    for (callback in eventListeners) {
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
    for (callback in eventListeners) {
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
    for (listener in eventListeners) {
      listener.onDetached(this)
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
    manager.notifyPlaybackRemoved(this)
    for (callback in eventListeners) {
      callback.onRemoved(this)
    }
    onRemove()
    eventListeners.clear()
    playerEventListeners.clear()
  }

  /**
   * Refresh the Playback internal state.
   */
  @MainThread
  internal fun performRefresh() {
    checkMainThread()
    val oldToken = this.token
    onRefresh()
    val newToken = this.token
    if (newToken != oldToken) {
      for (listener in eventListeners) {
        listener.onTokenUpdated(playback = this, token = newToken)
      }
    }
  }

  /**
   * Called after the Playback is attached, to make sure if it is activated or not.
   */
  @MainThread
  internal fun maybeActivated(): Boolean {
    performRefresh()
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
   * Refresh the Playback internal state and.
   */
  @CallSuper
  protected open fun onRefresh() {
    "Playback[${hexCode()}]_REFRESH".logInfo()
  }

  /**
   * Force this class to detach the current renderer.
   */
  protected abstract fun detachRenderer()

  @ExperimentalKohiiApi
  fun rebind(): RequestHandle {
    check(isAdded)
    return config.binder.bind(container, config)
  }

  @ExperimentalKohiiApi
  fun unbind(): Unit = manager.removePlayback(this)

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

  abstract class Token {

    open fun compare(
      other: Token,
      axis: Axis
    ): Int = 0

    companion object {
      val EMPTY = object : Token() {
        override fun toString(): String = "Token.EMPTY"
      }
    }
  }

  // TODO:
  //  - API for the specific Playback state that it should reset.
  class Config(
    internal val binder: Binder,
    internal var trigger: Float = 0.65f,
    internal var preload: Boolean = false,
  ) {

    internal val playbackEventListeners = arraySetOf<PlaybackEventListener>()
    internal val playerEventListeners = arraySetOf<PlayerEventListener>()

    fun setPreload(preload: Boolean) = apply {
      this.preload = preload
    }

    fun setTrigger(trigger: Float) = apply {
      this.trigger = trigger
    }

    fun addPlaybackEventListener(listener: PlaybackEventListener): Config = apply {
      playbackEventListeners.add(listener)
    }

    fun addPlayerEventListener(listener: PlayerEventListener): Config = apply {
      playerEventListeners.add(listener)
    }
  }

  companion object {
    @Throws(IllegalStateException::class)
    private fun Playback.checkState(expected: State): Unit = check(state == expected) {
      "Expected Playback($this) state: $expected, Actual state: $state"
    }
  }
}
