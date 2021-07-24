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

import androidx.annotation.CallSuper
import kohii.v2.common.logInfo
import kohii.v2.common.logWarn
import kohii.v2.internal.CallbackDelegate
import kohii.v2.internal.hexCode
import kotlin.LazyThreadSafetyMode.NONE

abstract class Playable(
  val tag: String,
  val data: Any,
  val rendererType: Class<*>
) {

  abstract val isPlaying: Boolean

  abstract val renderer: Any?

  // The current [Playback].
  val playback: Playback? get() = internalPlayback

  // The current [PlayableManager]. If null, this Playable must be scheduled for destruction.
  var manager: PlayableManager? = null
    internal set(value) {
      val prev = field
      if (prev !== value) prev?.removePlayable(this)
      field = value
      if (prev !== value) {
        value?.addPlayable(this)
        onManagerChanged(previous = prev, next = value)
      }
    }

  internal var internalPlayback: Playback? = null
    set(value) {
      // Update manager before updating the Playback.
      manager = value?.manager?.playableManager
      val prev = field
      field = value
      if (prev !== value) {
        onPlaybackChanged(previous = prev, next = value)
      }
    }

  private val playbackCallback: Playback.Callback by lazy(NONE) {
    CallbackDelegate(this)
  }

  init {
    @Suppress("LeakingThis")
    "Playable[${hexCode()}]_CREATED".logInfo()
  }

  /**
   * Called by the [Playback] to attach a renderer to this [Playable].
   */
  abstract fun onRendererAttached(renderer: Any?)

  /**
   * Called by the [Playback] to detach the renderer from this [Playable].
   */
  abstract fun onRendererDetached(renderer: Any?)

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
  protected open fun onPlaybackChanged(previous: Playback?, next: Playback?) {
    "Playable[${hexCode()}]_CHANGE_Playback [$previous → $next]".logWarn()
    previous?.removeCallback(playbackCallback)
    next?.addCallback(playbackCallback)
  }

  @CallSuper
  protected open fun onManagerChanged(previous: PlayableManager?, next: PlayableManager?) {
    "Playable[${hexCode()}]_CHANGE_Manager [$previous → $next]".logWarn()
  }
}
