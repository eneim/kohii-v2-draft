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

import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.collection.arraySetOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer.VideoAdPlayerCallback
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Playable.Controller
import kohii.v2.core.Playback.State.ACTIVATED
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.ATTACHED
import kohii.v2.core.Playback.State.CREATED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.exoplayer.ComponentsListener
import kohii.v2.exoplayer.ComponentsListeners
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
  private val lifecycleCallbacks = ArrayDeque(config.lifecycleCallbacks)
  private val playerEventListeners = ArrayDeque(config.playerEventListeners)

  private val internalComponentsListener: ComponentsListener = object : ComponentsListener {
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

  internal val componentsListeners: ComponentsListeners = with(config) {
    componentsListeners.add(internalComponentsListener)
    componentsListeners
  }

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
        for (callback in lifecycleCallbacks) {
          callback.onStateChanged(playback = this, fromState = oldState, toState = value)
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
  internal fun addLifecycleCallback(callback: LifecycleCallback) {
    checkMainThread()
    if (!lifecycleCallbacks.contains(callback)) {
      lifecycleCallbacks.add(callback)
    }
  }

  @Suppress("unused")
  @MainThread
  internal fun removeLifecycleCallback(callback: LifecycleCallback?) {
    checkMainThread()
    lifecycleCallbacks.remove(callback)
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
    for (callback in lifecycleCallbacks) {
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
    for (callback in lifecycleCallbacks) {
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
    for (callback in lifecycleCallbacks) {
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
    for (callback in lifecycleCallbacks) {
      callback.onDeactivated(this)
    }
    onDeactivate()
  }

  /**
   * Called by the [Manager] to perform detaching this Playback. This is called when the [View]
   * container is detached from the Window, or as part of the removal of this Playback.
   *
   * This method will call [onDetach], its state will be changed: [ATTACHED]->[ADDED]->[onDetach].
   */
  @MainThread
  internal fun performDetach() {
    checkMainThread()
    checkState(ATTACHED)
    state = ADDED
    for (callback in lifecycleCallbacks) {
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
    manager.notifyPlaybackRemoved(this)
    for (callback in lifecycleCallbacks) {
      callback.onRemoved(this)
    }
    onRemove()
    lifecycleCallbacks.clear()
    playerEventListeners.clear()
    componentsListeners.clear()
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
      for (callback in lifecycleCallbacks) {
        callback.onTokenUpdated(playback = this, token = newToken)
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
  @JvmOverloads
  fun rebind(callback: Binder.Callback? = null): RequestHandle {
    check(isAdded)
    return config.binder.bindInternal(
      container = container,
      config = config,
      callback = callback,
    )
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
      axis: Axis,
    ): Int = 0

    companion object {
      val EMPTY = object : Token() {
        override fun toString(): String = "Token.EMPTY"
      }
    }
  }

  class Config(
    internal val binder: Binder,
    internal var trigger: Float = 0.65f,
    internal var preload: Boolean = false,
  ) {

    internal val lifecycleCallbacks = arraySetOf<LifecycleCallback>()
    internal val playerEventListeners = arraySetOf<PlayerEventListener>()
    internal val componentsListeners = ComponentsListeners()

    fun setPreload(preload: Boolean) = apply {
      this.preload = preload
    }

    fun setTrigger(trigger: Float) = apply {
      this.trigger = trigger
    }

    fun addLifecycleCallback(callback: LifecycleCallback): Config = apply {
      lifecycleCallbacks.add(callback)
    }

    fun addPlayerEventListener(listener: PlayerEventListener): Config = apply {
      playerEventListeners.add(listener)
    }

    fun addPlayerListener(listener: Player.Listener): Config = apply {
      componentsListeners.add(ComponentsListener(listener))
    }

    fun addAdEventListener(listener: AdEventListener): Config = apply {
      componentsListeners.add(ComponentsListener(adEventListener = listener))
    }

    fun addAdErrorListener(listener: AdErrorListener): Config = apply {
      componentsListeners.add(ComponentsListener(adErrorListener = listener))
    }

    fun addVideoAdPlayerCallback(callback: VideoAdPlayerCallback): Config = apply {
      componentsListeners.add(ComponentsListener(videoAdPlayerCallback = callback))
    }
  }

  companion object {
    @Throws(IllegalStateException::class)
    private fun Playback.checkState(expected: State): Unit = check(state == expected) {
      "Expected Playback($this) state: $expected, Actual state: $state"
    }
  }
}
