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
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * Information that is used to restore an [ExoPlayer] instance.
 */
@OptIn(UnstableApi::class)
@Parcelize
class ExoPlayerExtras(
  @Player.State val playerState: Int,
  val playerParameters: PlayerParameters,
  val trackSelectionParameters: TrackSelectionParameters,
) : Parcelable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ExoPlayerExtras) return false

    if (playerState != other.playerState) return false
    if (playerParameters != other.playerParameters) return false
    if (trackSelectionParameters != other.trackSelectionParameters) return false

    return true
  }

  override fun hashCode(): Int {
    var result = playerState
    result = 31 * result + playerParameters.hashCode()
    result = 31 * result + trackSelectionParameters.hashCode()
    return result
  }

  override fun toString(): String {
    return "Extras(playerState=$playerState, " +
      "playerParameters=$playerParameters, " +
      "trackSelectionParameters=$trackSelectionParameters" +
      ")"
  }

  companion object : Parceler<ExoPlayerExtras> {

    val DEFAULT: ExoPlayerExtras = ExoPlayerExtras(
      playerState = Player.STATE_IDLE,
      playerParameters = PlayerParameters.DEFAULT,
      trackSelectionParameters = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT,
    )

    override fun create(parcel: Parcel): ExoPlayerExtras = ExoPlayerExtras(
      playerState = parcel.readInt(),
      playerParameters = parcel.readParcelable(PlayerParameters::class.java.classLoader)
        ?: PlayerParameters.DEFAULT,
      trackSelectionParameters = parcel
        .readBundle(TrackSelectionParameters::class.java.classLoader)
        ?.let(TrackSelectionParameters.CREATOR::fromBundle)
        ?: TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    )

    override fun ExoPlayerExtras.write(
      parcel: Parcel,
      flags: Int,
    ) {
      parcel.writeInt(playerState)
      parcel.writeParcelable(playerParameters, flags)
      parcel.writeBundle(trackSelectionParameters.toBundle())
    }
  }
}
