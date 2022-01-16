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

import android.os.Bundle
import androidx.annotation.CallSuper
import kohii.v2.core.Home.Companion.NO_TAG
import kohii.v2.core.Manager.Companion.DEFAULT_DESTRUCTION_DELAY_MS
import kohii.v2.core.Playback.Config
import kohii.v2.internal.hexCode
import kohii.v2.internal.logInfo
import kohii.v2.internal.logWarn
import kohii.v2.internal.playbackEventListener
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

abstract class Playable(
  val home: Home,
  val tag: String,
  val data: Any,
  val rendererType: Class<*>,
  initialManager: PlayableManager,
) {

  internal val internalId = UUID.randomUUID().toString()

  abstract val isStarted: Boolean

  abstract val isPlaying: Boolean

  abstract val renderer: Any?

  /**
   * The current [PlayableManager].
   */
  var manager: PlayableManager = initialManager
    internal set(value) {
      val prev = field
      if (prev !== value) {
        prev.removePlayable(this)
        value.addPlayable(this)
      }
      field = value
      if (prev !== value) {
        onManagerChanged(previous = prev, next = value)
      }
    }

  /**
   * The current [Playback] that is bound to this [Playable].
   */
  var playback: Playback? = null
    internal set(value) {
      val prev = field
      field = value
      if (value != null) manager = value.manager.playableManager
      if (prev !== value) onPlaybackChanged(previous = prev, next = value)
      if (value == null) home.destroyPlayableDelayed(this, DEFAULT_DESTRUCTION_DELAY_MS)
    }

  //region Manual control support
  internal val command = AtomicReference<Command>(null)
  //endregion

  private val playbackEventListener: PlaybackEventListener by playbackEventListener()

  init {
    "Playable[${hexCode()}]_CREATED".logInfo()
  }

  override fun toString(): String = "PB[${hexCode()}, ${rendererType.simpleName}, t=$tag, d=$data]"

  /**
   * Called when this [Playable] is bound to a new [Playback].
   *
   * This method is called right after the old [Playback] bound to this [Playable] is removed (if it
   * exists), and right before the new [Playback] is added.
   *
   * @param playback The [Playback] that this [Playable] is bound to.
   * @param state A [PlayableState] that can be used to initialize this [Playable]. This value is
   * `null` if the same [Playable] is reused.
   */
  @CallSuper
  open fun onBind(
    playback: Playback,
    state: PlayableState?
  ) {
    "Playable[${hexCode()}]_BIND, state=$state".logInfo()
  }

  /**
   * Returns the [Bundle] that contains the state of this [Playable]. This value will be used to
   * restore the [Playable] later.
   *
   * See [onRestoreState].
   */
  open fun onSaveState(): Bundle = Bundle.EMPTY

  /**
   * Restores this [Playable] to a previous state using the value from [state].
   *
   * See [onSaveState].
   */
  open fun onRestoreState(state: Bundle) = Unit

  /**
   * Called by the [Playback] to attach a renderer to this [Playable].
   */
  abstract fun onRendererAttached(renderer: Any?)

  /**
   * Called by the [Playback] to detach the renderer from this [Playable].
   */
  abstract fun onRendererDetached(renderer: Any?)

  /**
   * Returns the immediate state of this [Playable].
   */
  abstract fun currentState(): PlayableState

  @CallSuper
  open fun onPrepare(preload: Boolean) {
    "Playable[${hexCode()}]_PREPARE [preload=$preload]".logInfo()
  }

  @CallSuper
  open fun onReady() {
    "Playable[${hexCode()}]_READY".logInfo()
  }

  @CallSuper
  open fun onStart() {
    "Playable[${hexCode()}]_START".logInfo()
    playback?.onStarted()
  }

  @CallSuper
  open fun onPause() {
    playback?.onPaused()
    "Playable[${hexCode()}]_PAUSE".logInfo()
  }

  /**
   * Reset the state of this [Playable] so that it can start from the beginning. Another call to
   * [onPrepare] is required to restart the playback.
   */
  open fun onReset() = Unit

  /**
   * Releases the resource hold by this [Playable]. This method is called right before the
   * destruction of the [Playable] instance. The [Playable] instance is no longer used again after
   * this method is called.
   */
  abstract fun onRelease()

  /**
   * Called when the [Playback] bound to this [Playable] is changed.
   *
   * It can be when a [Playable] is bound to a [Playback] for the first time (in which case
   * [previous] is null and [next] is not null), or is switched from a [Playback] to another
   * [Playback] (in which case both [previous] and [next] are not null), or is unbound from a
   * [Playback] (in which case [previous] is not null and [next] is null). Note that there should
   * not be the case both [previous] and [next] are both null.
   */
  @CallSuper
  protected open fun onPlaybackChanged(
    previous: Playback?,
    next: Playback?
  ) {
    "Playable[${hexCode()}]_CHANGE_Playback [$previous → $next]".logWarn()
    previous?.removePlaybackEventListener(playbackEventListener)
    next?.addPlaybackEventListener(playbackEventListener)
  }

  @CallSuper
  protected open fun onManagerChanged(
    previous: PlayableManager,
    next: PlayableManager
  ) {
    "Playable[${hexCode()}]_CHANGE_Manager [$previous → $next]".logWarn()
  }

  /**
   * Supported commands sent from the client.
   */
  enum class Command {
    STARTED_BY_USER,
    PAUSED_BY_USER,
  }

  interface Controller {

    /**
     * Starts a playback as long as the container reaches the [Config.trigger] value.
     */
    fun play() = Unit

    /**
     * Pauses the [Playback]. Until another call to [play], or the underneath [Playable] is rebound
     * to another container, it will not start again.
     */
    fun pause() = Unit

    /**
     * Clears any [Command] set to the [Playable].
     */
    fun auto() = Unit
  }
}

internal sealed class PlayableKey(val tag: String) {

  object Empty : PlayableKey(NO_TAG)

  class Data(tag: String) : PlayableKey(tag = tag)
}
