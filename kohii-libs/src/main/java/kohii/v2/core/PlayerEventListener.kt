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

import androidx.media3.common.Player
import androidx.media3.common.Player.State
import androidx.media3.common.VideoSize

/**
 * Derived from [Player.Listener].
 */
interface PlayerEventListener {

  /**
   * See [Player.Listener.onPlaybackStateChanged].
   */
  fun onStateChanged(
    playback: Playback,
    @State state: Int,
  ) = Unit

  /**
   * See [Player.Listener.onVideoSizeChanged]
   */
  fun onVideoSizeChanged(
    playback: Playback,
    videoSize: VideoSize,
  ) = Unit
}
