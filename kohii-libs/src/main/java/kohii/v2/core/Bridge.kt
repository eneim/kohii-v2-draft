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

import kohii.v2.exoplayer.ComponentsListener

interface Bridge<RENDERER : Any> {

  val isStarted: Boolean

  val isPlaying: Boolean

  var renderer: RENDERER?

  var playableState: PlayableState

  var controller: Playable.Controller

  /**
   * Prepare the resource for a media. This method should:
   * - Request for a Player instance.
   * - Configure callbacks for the Player implementation.
   * - If client request to prepare MediaSource, then prepare it.
   *
   * This method must be called before [ready].
   *
   * @param loadSource if `true`, also prepare the MediaSource when preparing the Player,
   * if `false` just do nothing for the MediaSource.
   */
  fun prepare(loadSource: Boolean)

  /**
   * Ensure the resource is ready to play.
   */
  fun ready()

  /**
   * Starts the playback.
   */
  fun play()

  /**
   * Pause the playback.
   */
  fun pause()

  /**
   * Resets the playback resource, so that it can start all over again. Another call to [prepare]
   * is required to restart the playback.
   *
   * @see [Playable.onReset]
   */
  fun reset()

  /**
   * Release all resources. The [Playable] may call this when it is not active, but not yet
   * destroyed.
   *
   * @see [Playable.onRelease]
   */
  fun release()

  fun addComponentsListener(listener: ComponentsListener)

  fun removeComponentsListener(listener: ComponentsListener?)
}
