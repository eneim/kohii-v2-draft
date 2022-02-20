/*
 * Copyright (c) 2022. Nam Nguyen, nam@ene.im
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

package kohii.v2.internal

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import kohii.v2.exoplayer.PlayerParameters

internal class PlaybackInfo(
  val mediaItemIndex: Int = C.INDEX_UNSET,
  val currentPositionMillis: Long = C.POSITION_UNSET.toLong(),
  val playerParameters: PlayerParameters = PlayerParameters.DEFAULT,
  val trackSelectionParameters: TrackSelectionParameters =
    TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT,
) {

  companion object {
    val EMPTY = PlaybackInfo()
  }
}
