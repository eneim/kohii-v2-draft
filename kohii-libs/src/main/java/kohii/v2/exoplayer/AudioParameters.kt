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

package kohii.v2.exoplayer

import android.os.Parcel
import android.os.Parcelable
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * Parameters used by [ExoPlayer.setAudioAttributes].
 */
@Parcelize
class AudioParameters(
  val handleAudioFocus: Boolean = true,
  val audioAttributes: AudioAttributes = AudioAttributes.DEFAULT,
) : Parcelable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AudioParameters) return false

    if (handleAudioFocus != other.handleAudioFocus) return false
    if (audioAttributes != other.audioAttributes) return false

    return true
  }

  override fun hashCode(): Int {
    var result = handleAudioFocus.hashCode()
    result = 31 * result + audioAttributes.hashCode()
    return result
  }

  companion object : Parceler<AudioParameters> {

    val DEFAULT: AudioParameters = AudioParameters()

    override fun create(parcel: Parcel): AudioParameters = AudioParameters(
      handleAudioFocus = parcel.readInt() == 1,
      audioAttributes = parcel.readBundle(AudioParameters::class.java.classLoader)
        ?.let(AudioAttributes.CREATOR::fromBundle) ?: AudioAttributes.DEFAULT
    )

    override fun AudioParameters.write(
      parcel: Parcel,
      flags: Int,
    ) {
      parcel.writeInt(if (handleAudioFocus) 1 else 0)
      parcel.writeBundle(audioAttributes.toBundle())
    }
  }
}
