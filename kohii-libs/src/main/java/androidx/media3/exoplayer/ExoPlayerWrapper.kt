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

package androidx.media3.exoplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import kohii.v2.exoplayer.AudioParameters
import kohii.v2.exoplayer.PlayerParameters
import kohii.v2.exoplayer.PlayerViewBridge

/**
 * A wrapper for [ExoPlayer] that also exposes the [DefaultMediaSourceFactory] instance if it is use
 * to create the player instance. This is used by the [PlayerViewBridge] to setup for Ad
 * playback.
 */
class ExoPlayerWrapper private constructor(
  private val player: ExoPlayer,
  val mediaSourceFactory: DefaultMediaSourceFactory?,
) : ExoPlayer by player {

  private constructor(
    builder: ExoPlayer.Builder,
    mediaSourceFactory: MediaSource.Factory,
  ) : this(
    player = builder
      .setMediaSourceFactory(mediaSourceFactory)
      .build(),
    mediaSourceFactory = mediaSourceFactory as? DefaultMediaSourceFactory,
  )

  constructor(builder: ExoPlayer.Builder) : this(
    builder = builder,
    mediaSourceFactory = builder.mediaSourceFactorySupplier.get(),
  )

  private var audioParameters: AudioParameters = AudioParameters.DEFAULT
    set(value) {
      field = value
      player.setAudioAttributes(value.audioAttributes, value.handleAudioFocus)
    }

  var playerParameters: PlayerParameters = PlayerParameters.DEFAULT
    get() = PlayerParameters(
      volume = volume,
      repeatMode = repeatMode,
      shuffleModeEnabled = shuffleModeEnabled,
      audioParameters = audioParameters,
      playbackParameters = playbackParameters,
    )
    set(value) {
      field = value
      volume = value.volume
      repeatMode = value.repeatMode
      shuffleModeEnabled = value.shuffleModeEnabled
      audioParameters = value.audioParameters
      playbackParameters = value.playbackParameters
    }

  override fun setAudioAttributes(
    audioAttributes: AudioAttributes,
    handleAudioFocus: Boolean,
  ) {
    this.audioParameters = AudioParameters(
      audioAttributes = audioAttributes,
      handleAudioFocus = handleAudioFocus
    )
  }
}

internal var ExoPlayer.parameters: PlayerParameters
  get() = if (this is ExoPlayerWrapper) {
    playerParameters
  } else {
    PlayerParameters(
      volume = volume,
      repeatMode = repeatMode,
      shuffleModeEnabled = shuffleModeEnabled,
      audioParameters = AudioParameters(handleAudioFocus = true, audioAttributes = audioAttributes),
      playbackParameters = playbackParameters,
    )
  }
  set(value) {
    if (this is ExoPlayerWrapper) playerParameters = value
    else {
      volume = value.volume
      repeatMode = value.repeatMode
      shuffleModeEnabled = value.shuffleModeEnabled
      setAudioAttributes(
        value.audioParameters.audioAttributes,
        value.audioParameters.handleAudioFocus
      )
      playbackParameters = playbackParameters
    }
  }
