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

import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.Commands
import androidx.media3.common.Player.Events
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.Timeline
import androidx.media3.common.TracksInfo
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer.VideoAdPlayerCallback
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate

private val DEFAULT_PLAYER_LISTENER: Player.Listener = object : Player.Listener {}
private val DEFAULT_AD_EVENT_LISTENER: AdEventListener = AdEventListener { }
private val DEFAULT_AD_ERROR_LISTENER: AdErrorListener = AdErrorListener { }
private val DEFAULT_VIDEO_AD_CALLBACK: VideoAdPlayerCallback =
  object : DefaultVideoAdPlayerCallback {}

/**
 * Combined the listener interfaces for Video playback and Ad playback.
 */
interface ComponentsListener :
  Player.Listener,
  AdEventListener,
  AdErrorListener,
  DefaultVideoAdPlayerCallback {

  //region AdEventListener
  override fun onAdEvent(event: AdEvent) = Unit
  //endregion

  //region AdErrorListener
  override fun onAdError(error: AdErrorEvent?) = Unit
  //endregion
}

@UnstableApi
@JvmSynthetic
internal fun ComponentsListener(
  playerListener: Player.Listener = DEFAULT_PLAYER_LISTENER,
  adEventListener: AdEventListener = DEFAULT_AD_EVENT_LISTENER,
  adErrorListener: AdErrorListener = DEFAULT_AD_ERROR_LISTENER,
  videoAdPlayerCallback: VideoAdPlayerCallback = DEFAULT_VIDEO_AD_CALLBACK,
): ComponentsListener = object : ComponentsListener {

  //region AdEventListener
  override fun onAdEvent(event: AdEvent) = adEventListener.onAdEvent(event)
  //endregion

  //region AdErrorListener
  override fun onAdError(error: AdErrorEvent?) = adErrorListener.onAdError(error)
  //endregion

  //region VideoAdPlayerCallback
  override fun onAdProgress(
    mediaInfo: AdMediaInfo,
    progressUpdate: VideoProgressUpdate,
  ) = videoAdPlayerCallback.onAdProgress(mediaInfo, progressUpdate)

  override fun onVolumeChanged(
    mediaInfo: AdMediaInfo,
    percentage: Int,
  ) = videoAdPlayerCallback.onVolumeChanged(mediaInfo, percentage)

  override fun onBuffering(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onBuffering(mediaInfo)
  override fun onContentComplete() = videoAdPlayerCallback.onContentComplete()
  override fun onEnded(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onEnded(mediaInfo)
  override fun onError(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onError(mediaInfo)
  override fun onLoaded(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onLoaded(mediaInfo)
  override fun onPause(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onPause(mediaInfo)
  override fun onPlay(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onPlay(mediaInfo)
  override fun onResume(mediaInfo: AdMediaInfo) = videoAdPlayerCallback.onResume(mediaInfo)
  //endregion

  //region Player.Listener
  override fun onVolumeChanged(volume: Float) = playerListener.onVolumeChanged(volume)

  override fun onTimelineChanged(
    timeline: Timeline,
    reason: Int,
  ) = playerListener.onTimelineChanged(timeline, reason)

  override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
  ) = playerListener.onMediaItemTransition(mediaItem, reason)

  override fun onTracksInfoChanged(tracksInfo: TracksInfo) =
    playerListener.onTracksInfoChanged(tracksInfo)

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) =
    playerListener.onMediaMetadataChanged(mediaMetadata)

  override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) =
    playerListener.onPlaylistMetadataChanged(mediaMetadata)

  override fun onAvailableCommandsChanged(availableCommands: Commands) =
    playerListener.onAvailableCommandsChanged(availableCommands)

  override fun onPlaybackStateChanged(playbackState: Int) =
    playerListener.onPlaybackStateChanged(playbackState)

  override fun onPlayWhenReadyChanged(
    playWhenReady: Boolean,
    reason: Int,
  ) = playerListener.onPlayWhenReadyChanged(playWhenReady, reason)

  override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
    playerListener.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
    playerListener.onShuffleModeEnabledChanged(shuffleModeEnabled)

  override fun onPlayerError(error: PlaybackException) = playerListener.onPlayerError(error)

  override fun onPlayerErrorChanged(error: PlaybackException?) =
    playerListener.onPlayerErrorChanged(error)

  override fun onPositionDiscontinuity(
    oldPosition: PositionInfo,
    newPosition: PositionInfo,
    reason: Int,
  ) = playerListener.onPositionDiscontinuity(oldPosition, newPosition, reason)

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
    playerListener.onPlaybackParametersChanged(playbackParameters)

  override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) =
    playerListener.onSeekBackIncrementChanged(seekBackIncrementMs)

  override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) =
    playerListener.onSeekForwardIncrementChanged(seekForwardIncrementMs)

  override fun onEvents(
    player: Player,
    events: Events,
  ) = playerListener.onEvents(player, events)

  override fun onAudioSessionIdChanged(audioSessionId: Int) =
    playerListener.onAudioSessionIdChanged(audioSessionId)

  override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) =
    playerListener.onAudioAttributesChanged(audioAttributes)

  override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) =
    playerListener.onSkipSilenceEnabledChanged(skipSilenceEnabled)

  override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) =
    playerListener.onDeviceInfoChanged(deviceInfo)

  override fun onDeviceVolumeChanged(
    volume: Int,
    muted: Boolean,
  ) = playerListener.onDeviceVolumeChanged(volume, muted)

  override fun onVideoSizeChanged(videoSize: VideoSize) =
    playerListener.onVideoSizeChanged(videoSize)

  override fun onSurfaceSizeChanged(
    width: Int,
    height: Int,
  ) = playerListener.onSurfaceSizeChanged(width, height)

  override fun onRenderedFirstFrame() = playerListener.onRenderedFirstFrame()
  override fun onCues(cues: MutableList<Cue>) = playerListener.onCues(cues)
  override fun onMetadata(metadata: Metadata) = playerListener.onMetadata(metadata)
  override fun onIsLoadingChanged(isLoading: Boolean) = playerListener.onIsLoadingChanged(isLoading)
  override fun onIsPlayingChanged(isPlaying: Boolean) = playerListener.onIsPlayingChanged(isPlaying)
  override fun onRepeatModeChanged(repeatMode: Int) = playerListener.onRepeatModeChanged(repeatMode)
  //endregion
}
