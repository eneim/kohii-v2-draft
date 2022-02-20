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
import androidx.annotation.FloatRange
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * Common parameters used by an [ExoPlayer] instance.
 */
@Parcelize
class PlayerParameters(
  @FloatRange(from = 0.0, to = 1.0) val volume: Float = 1.0f,
  @Player.RepeatMode val repeatMode: Int = Player.REPEAT_MODE_OFF,
  val shuffleModeEnabled: Boolean = false,
  val audioParameters: AudioParameters = AudioParameters.DEFAULT,
  val playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT,
) : Parcelable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PlayerParameters) return false

    if (volume != other.volume) return false
    if (repeatMode != other.repeatMode) return false
    if (shuffleModeEnabled != other.shuffleModeEnabled) return false
    if (audioParameters != other.audioParameters) return false
    if (playbackParameters != other.playbackParameters) return false

    return true
  }

  override fun hashCode(): Int {
    var result = volume.hashCode()
    result = 31 * result + repeatMode
    result = 31 * result + shuffleModeEnabled.hashCode()
    result = 31 * result + audioParameters.hashCode()
    result = 31 * result + playbackParameters.hashCode()
    return result
  }

  override fun toString(): String {
    return "PlayerParameters(volume=$volume, " +
      "repeatMode=$repeatMode, " +
      "shuffleModeEnabled=$shuffleModeEnabled, " +
      "audioParameters=$audioParameters, " +
      "playbackParameters=$playbackParameters" +
      ")"
  }

  companion object : Parceler<PlayerParameters> {

    val DEFAULT = PlayerParameters()

    override fun create(parcel: Parcel): PlayerParameters = PlayerParameters(
      volume = parcel.readFloat(),
      repeatMode = parcel.readInt(),
      shuffleModeEnabled = parcel.readInt() == 1,
      audioParameters = parcel.readParcelable(AudioParameters::class.java.classLoader)
        ?: AudioParameters.DEFAULT,
      playbackParameters = parcel.readBundle(PlaybackParameters::class.java.classLoader)
        ?.let(PlaybackParameters.CREATOR::fromBundle) ?: PlaybackParameters.DEFAULT,
    )

    override fun PlayerParameters.write(
      parcel: Parcel,
      flags: Int,
    ) {
      parcel.writeFloat(volume)
      parcel.writeInt(repeatMode)
      parcel.writeInt(if (shuffleModeEnabled) 1 else 0)
      parcel.writeParcelable(audioParameters, flags)
      parcel.writeBundle(playbackParameters.toBundle())
    }
  }
}
