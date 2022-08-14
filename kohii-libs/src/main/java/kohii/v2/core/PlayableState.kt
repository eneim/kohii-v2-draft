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
  class Active(
    val progress: PlayerProgress,
    val extras: Parcelable,
  ) : PlayableState, Parcelable {

    override fun toBundle(): Bundle = bundleOf(KEY_PLAYABLE_STATE to this as Parcelable)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Active) return false

      if (progress != other.progress) return false
      if (extras != other.extras) return false

      return true
    }

    override fun hashCode(): Int {
      var result = progress.hashCode()
      result = 31 * result + extras.hashCode()
      return result
    }

    override fun toString(): String {
      return "ActivePlayable(progress=$progress, extras=$extras)"
    }

    companion object {
      val DEFAULT: Active = Active(
        progress = PlayerProgress.DEFAULT,
        extras = Bundle.EMPTY,
      )
    }
  }

  companion object {
    private const val KEY_PLAYABLE_STATE = "KEY_PLAYABLE_STATE"

    fun Bundle.toPlayableState(): PlayableState? {
      return getParcelable(
        /* key = */ KEY_PLAYABLE_STATE,
        /* clazz = */ Active::class.java
      ) ?: getString(KEY_PLAYABLE_STATE)?.let { state ->
        when (state) {
          Initialized.toString() -> Initialized
          Idle.toString() -> Idle
          Ended.toString() -> Ended
          else -> null
        }
      }
    }
  }
}
