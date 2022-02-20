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

package kohii.v2.core

import android.os.Parcelable
import com.google.android.exoplayer2.C
import kotlinx.parcelize.Parcelize

/**
 * Common progress data of an active player. Some properties are self-explained.
 *
 * @property isStarted The player is started. It may be loading the data or actually playing the
 * content. If `false`, the player is paused ot is not prepared.
 */
@Parcelize
class PlayerProgress(
  val isStarted: Boolean,
  val totalDurationMillis: Long, // Ads + Content, C.TIME_UNSET if unknown.
  val contentDurationMillis: Long, // Content only, C.TIME_UNSET if unknown.
  val currentPositionMillis: Long, // C.TIME_UNSET if unknown.
  val currentMediaItemIndex: Int, // C.INDEX_UNSET
) : Parcelable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PlayerProgress) return false

    if (isStarted != other.isStarted) return false
    if (totalDurationMillis != other.totalDurationMillis) return false
    if (contentDurationMillis != other.contentDurationMillis) return false
    if (currentPositionMillis != other.currentPositionMillis) return false
    if (currentMediaItemIndex != other.currentMediaItemIndex) return false

    return true
  }

  override fun hashCode(): Int {
    var result = isStarted.hashCode()
    result = 31 * result + totalDurationMillis.hashCode()
    result = 31 * result + contentDurationMillis.hashCode()
    result = 31 * result + currentPositionMillis.hashCode()
    result = 31 * result + currentMediaItemIndex
    return result
  }

  override fun toString(): String {
    return "PlayerProgress(isStarted=$isStarted, " +
      "totalDurationMillis=$totalDurationMillis, " +
      "contentDurationMillis=$contentDurationMillis, " +
      "currentPositionMillis=$currentPositionMillis, " +
      "currentMediaItemIndex=$currentMediaItemIndex" +
      ")"
  }

  companion object {
    val DEFAULT: PlayerProgress = PlayerProgress(
      isStarted = false,
      totalDurationMillis = C.TIME_UNSET,
      contentDurationMillis = C.TIME_UNSET,
      currentPositionMillis = C.TIME_UNSET,
      currentMediaItemIndex = C.INDEX_UNSET,
    )
  }
}
