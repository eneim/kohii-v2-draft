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

import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.google.android.exoplayer2.DeviceInfo
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Commands
import com.google.android.exoplayer2.Player.Events
import com.google.android.exoplayer2.Player.PositionInfo
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.TracksInfo
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.video.VideoSize
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A [ComponentsListener] which is also a [MutableSet] of the [ComponentsListener].
 */
class ComponentsListeners internal constructor(
  listeners: MutableSet<ComponentsListener>,
) : ComponentsListener, MutableSet<ComponentsListener> by listeners {

  constructor() : this(listeners = CopyOnWriteArraySet())

  //region AdEventListener
  override fun onAdEvent(event: AdEvent) = forEach { it.onAdEvent(event) }
  //endregion

  //region AdErrorListener
  override fun onAdError(error: AdErrorEvent?) = forEach { it.onAdError(error) }
  //endregion

  //region VideoAdPlayerCallback
  override fun onAdProgress(
    mediaInfo: AdMediaInfo,
    progressUpdate: VideoProgressUpdate,
  ) = forEach { it.onAdProgress(mediaInfo, progressUpdate) }

  override fun onVolumeChanged(
    mediaInfo: AdMediaInfo,
    percentage: Int,
  ) = forEach { it.onVolumeChanged(mediaInfo, percentage) }

  override fun onBuffering(mediaInfo: AdMediaInfo) = forEach { it.onBuffering(mediaInfo) }
  override fun onContentComplete() = forEach { it.onContentComplete() }
  override fun onEnded(mediaInfo: AdMediaInfo) = forEach { it.onEnded(mediaInfo) }
  override fun onError(mediaInfo: AdMediaInfo) = forEach { it.onError(mediaInfo) }
  override fun onLoaded(mediaInfo: AdMediaInfo) = forEach { it.onLoaded(mediaInfo) }
  override fun onPause(mediaInfo: AdMediaInfo) = forEach { it.onPause(mediaInfo) }
  override fun onPlay(mediaInfo: AdMediaInfo) = forEach { it.onPlay(mediaInfo) }
  override fun onResume(mediaInfo: AdMediaInfo) = forEach { it.onResume(mediaInfo) }
  //endregion

  //region Player.Listener
  override fun onVideoSizeChanged(videoSize: VideoSize): Unit =
    forEach { it.onVideoSizeChanged(videoSize) }

  override fun onSurfaceSizeChanged(
    width: Int,
    height: Int,
  ): Unit = forEach { it.onSurfaceSizeChanged(width, height) }

  override fun onRenderedFirstFrame(): Unit = forEach { it.onRenderedFirstFrame() }

  override fun onAudioSessionIdChanged(audioSessionId: Int): Unit =
    forEach { it.onAudioSessionIdChanged(audioSessionId) }

  override fun onAudioAttributesChanged(audioAttributes: AudioAttributes): Unit =
    forEach { it.onAudioAttributesChanged(audioAttributes) }

  override fun onVolumeChanged(volume: Float): Unit = forEach { it.onVolumeChanged(volume) }

  override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean): Unit =
    forEach { it.onSkipSilenceEnabledChanged(skipSilenceEnabled) }

  override fun onCues(cues: MutableList<Cue>): Unit = forEach { it.onCues(cues) }
  override fun onMetadata(metadata: Metadata): Unit = forEach { it.onMetadata(metadata) }

  override fun onDeviceInfoChanged(deviceInfo: DeviceInfo): Unit =
    forEach { it.onDeviceInfoChanged(deviceInfo) }

  override fun onDeviceVolumeChanged(
    volume: Int,
    muted: Boolean,
  ): Unit = forEach { it.onDeviceVolumeChanged(volume, muted) }

  override fun onTimelineChanged(
    timeline: Timeline,
    reason: Int,
  ): Unit = forEach { it.onTimelineChanged(timeline, reason) }

  override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
  ): Unit = forEach { it.onMediaItemTransition(mediaItem, reason) }

  override fun onTracksInfoChanged(tracksInfo: TracksInfo): Unit =
    forEach { it.onTracksInfoChanged(tracksInfo) }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata): Unit =
    forEach { it.onMediaMetadataChanged(mediaMetadata) }

  override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata): Unit =
    forEach { it.onPlaylistMetadataChanged(mediaMetadata) }

  override fun onIsLoadingChanged(isLoading: Boolean): Unit =
    forEach { it.onIsLoadingChanged(isLoading) }

  override fun onAvailableCommandsChanged(availableCommands: Commands): Unit =
    forEach { it.onAvailableCommandsChanged(availableCommands) }

  override fun onPlaybackStateChanged(playbackState: Int): Unit =
    forEach { it.onPlaybackStateChanged(playbackState) }

  override fun onPlayWhenReadyChanged(
    playWhenReady: Boolean,
    reason: Int,
  ): Unit = forEach { it.onPlayWhenReadyChanged(playWhenReady, reason) }

  override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int): Unit =
    forEach { it.onPlaybackSuppressionReasonChanged(playbackSuppressionReason) }

  override fun onIsPlayingChanged(isPlaying: Boolean): Unit =
    forEach { it.onIsPlayingChanged(isPlaying) }

  override fun onRepeatModeChanged(repeatMode: Int): Unit =
    forEach { it.onRepeatModeChanged(repeatMode) }

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean): Unit =
    forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }

  override fun onPlayerError(error: PlaybackException): Unit = forEach { it.onPlayerError(error) }

  override fun onPlayerErrorChanged(error: PlaybackException?): Unit =
    forEach { it.onPlayerErrorChanged(error) }

  override fun onPositionDiscontinuity(
    oldPosition: PositionInfo,
    newPosition: PositionInfo,
    reason: Int,
  ): Unit = forEach { it.onPositionDiscontinuity(oldPosition, newPosition, reason) }

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters): Unit =
    forEach { it.onPlaybackParametersChanged(playbackParameters) }

  override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long): Unit =
    forEach { it.onSeekBackIncrementChanged(seekBackIncrementMs) }

  override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long): Unit =
    forEach { it.onSeekForwardIncrementChanged(seekForwardIncrementMs) }

  override fun onEvents(
    player: Player,
    events: Events,
  ): Unit = forEach { it.onEvents(player, events) }
  //endregion
}
