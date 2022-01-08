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
import androidx.annotation.MainThread
import kohii.v2.core.Playback.State
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.ATTACHED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.core.Playback.Token

/**
 * Class that can receive the changes of a Playback lifecycle via callback methods.
 */
interface PlaybackEventListener {

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

  /**
   * Called when the [playback] is refreshed and its [Token] is updated. The value of [token] is not
   * necessarily different to the previous one.
   */
  @MainThread
  fun onTokenUpdated(
    playback: Playback,
    token: Token
  ) = Unit

  /**
   * Called when the state of [playback] is changed.
   *
   * @param fromState the old [Playback.State].
   * @param toState the new [Playback.State].
   */
  @MainThread
  fun onStateChanged(
    playback: Playback,
    fromState: State,
    toState: State
  ) = Unit
}
