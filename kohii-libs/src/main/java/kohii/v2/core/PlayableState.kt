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
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.Player
import kotlinx.parcelize.Parcelize

/**
 * Represents an immediate state of a [Playable].
 */
sealed interface PlayableState {

  fun toBundle(): Bundle = bundleOf(KEY_PLAYABLE_STATE to toString())

  object Initialized : PlayableState {
    override fun toString(): String = "Initialized"
  }

  object Idle : PlayableState {
    override fun toString(): String = "Idle"
  }

  object Ended : PlayableState {
    override fun toString(): String = "Ended"
  }

  @Parcelize
  class Progress(
    @Player.State val playerState: Int,
    val totalDurationMillis: Long, // Ads + Content, Long.MIN_VALUE if unknown.
    val contentDurationMillis: Long, // Content only, Long.MIN_VALUE if unknown.
    val currentPositionMillis: Long,
    val currentMediaItemIndex: Int,
    val isStarted: Boolean,
    val isPlaying: Boolean,
  ) : PlayableState, Parcelable {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Progress

      if (playerState != other.playerState) return false
      if (totalDurationMillis != other.totalDurationMillis) return false
      if (contentDurationMillis != other.contentDurationMillis) return false
      if (currentPositionMillis != other.currentPositionMillis) return false
      if (currentMediaItemIndex != other.currentMediaItemIndex) return false
      if (isStarted != other.isStarted) return false
      if (isPlaying != other.isPlaying) return false

      return true
    }

    override fun hashCode(): Int {
      var result = playerState
      result = 31 * result + totalDurationMillis.hashCode()
      result = 31 * result + contentDurationMillis.hashCode()
      result = 31 * result + currentPositionMillis.hashCode()
      result = 31 * result + currentMediaItemIndex.hashCode()
      result = 31 * result + isStarted.hashCode()
      result = 31 * result + isPlaying.hashCode()
      return result
    }

    override fun toString(): String {
      return "Progress(playerState=$playerState, " +
        "totalDurationMillis=$totalDurationMillis, " +
        "contentDurationMillis=$contentDurationMillis, " +
        "currentPositionMillis=$currentPositionMillis, " +
        "currentMediaItemIndex=$currentMediaItemIndex, " +
        "isStarted=$isStarted, " +
        "isPlaying=$isPlaying)"
    }

    override fun toBundle(): Bundle = bundleOf(KEY_PLAYABLE_STATE to this as Parcelable)
  }

  companion object {
    private const val KEY_PLAYABLE_STATE = "KEY_PLAYABLE_STATE"

    fun Bundle.toPlayableState(): PlayableState? = with(get(KEY_PLAYABLE_STATE)) {
      when (this) {
        is Progress -> return@with this
        is String -> return@with when (this) {
          Initialized.toString() -> Initialized
          Idle.toString() -> Idle
          Ended.toString() -> Ended
          else -> null
        }
        else -> return@with null
      }
    }
  }
}
