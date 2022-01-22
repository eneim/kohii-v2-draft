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

package kohii.v2.exoplayer

import android.content.Context
import android.util.Pair
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import com.google.ads.interactivemedia.v3.api.FriendlyObstructionPurpose
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerWrapper
import com.google.android.exoplayer2.ForwardingPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Events
import com.google.android.exoplayer2.TracksInfo
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.ui.AdOverlayInfo
import com.google.android.exoplayer2.ui.AdOverlayInfo.Purpose
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import kohii.v2.R
import kohii.v2.core.AbstractBridge
import kohii.v2.core.Bridge
import kohii.v2.core.PlayableState
import kohii.v2.core.PlayerPool
import kohii.v2.internal.PlaybackInfo
import kohii.v2.internal.hexCode
import kohii.v2.internal.logInfo

/**
 * A [Bridge] that works with [StyledPlayerView] and [Player].
 */
internal class StyledPlayerViewBridge(
  context: Context,
  private val mediaItems: List<MediaItem>,
  private val playerPool: PlayerPool<ExoPlayer>,
) : AbstractBridge<StyledPlayerView>(),
  Player.Listener,
  ErrorMessageProvider<Throwable> {

  private val appContext: Context = context.applicationContext
  private val playbackStatsListener = PlaybackStatsListener(false, null)

  private var imaSetupBundle: ImaSetupBundle? = null
  private var internalPlayer: InternalPlayerWrapper? = null
    set(value) {
      if (field !== value) {
        field?.wrappedPlayer?.removeAnalyticsListener(playbackStatsListener)
        field?.wrappedPlayer?.removeListener(playerListeners)
        field = value
        field?.wrappedPlayer?.addListener(playerListeners)
        field?.wrappedPlayer?.addAnalyticsListener(playbackStatsListener)
      }
    }

  private var lastSeenTracksInfo: TracksInfo = TracksInfo.EMPTY
  private var playerPrepared = false

  // A temporary data for restoration only. It will be cleared after the restoration.
  private var playbackRestoreInfo: PlaybackInfo = PlaybackInfo.EMPTY

  override var renderer: StyledPlayerView? = null
    set(value) {
      if (field === value) return

      val imaBundle = imaSetupBundle

      field?.let { playerView ->
        playerView.setErrorMessageProvider(null)
        playerView.setCustomErrorMessage(null)
        if (imaBundle != null) {
          playerView.adViewGroup.removeView(imaBundle.adViewGroup)
          imaBundle.adsLoader.adDisplayContainer?.unregisterAllFriendlyObstructions()
        }
      }

      internalPlayer?.let { player ->
        StyledPlayerView.switchTargetView(player, field, value)
      }

      if (value != null) {
        value.setErrorMessageProvider(this)
        if (imaBundle != null) {
          value.adViewGroup.addView(imaBundle.adViewGroup)
          imaBundle.adsLoader.adDisplayContainer?.let { container ->
            for (adOverlayInfo in value.adOverlayInfos) {
              container.registerFriendlyObstruction(
                ImaSdkFactory.getInstance().createFriendlyObstruction(
                  adOverlayInfo.view,
                  getFriendlyObstructionPurpose(adOverlayInfo.purpose),
                  adOverlayInfo.reasonDetail
                )
              )
            }
          }
        }
      }
      field = value
    }

  override var playableState: PlayableState = PlayableState.Initialized
    get() {
      val player = internalPlayer ?: return PlayableState.Idle
      return when (val playerState = player.playbackState) {
        Player.STATE_ENDED -> PlayableState.Ended
        Player.STATE_IDLE -> PlayableState.Idle
        else -> PlayableState.Progress(
          playerState = playerState,
          totalDurationMillis = player.duration,
          contentDurationMillis = player.contentDuration,
          currentPositionMillis = player.currentPosition,
          currentMediaItemIndex = player.currentMediaItemIndex,
          isStarted = isStarted,
          isPlaying = player.isPlaying,
        )
      }
    }
    set(value) {
      field = value
      if (value is PlayableState.Progress) {
        val player: Player? = internalPlayer
        if (player != null) {
          if (value.currentMediaItemIndex != C.INDEX_UNSET) {
            player.seekTo(value.currentMediaItemIndex, value.currentPositionMillis)
          }
        } else {
          playbackRestoreInfo = PlaybackInfo(
            mediaItemIndex = value.currentMediaItemIndex,
            currentPositionMillis = value.currentPositionMillis
          )
        }
      }
    }

  override val isStarted: Boolean
    get() {
      val player = internalPlayer ?: return false
      return player.playWhenReady &&
        player.playbackState in Player.STATE_BUFFERING..Player.STATE_READY
    }

  override val isPlaying: Boolean
    get() {
      val player = internalPlayer ?: return false
      return player.isPlaying
    }

  override fun prepare(loadSource: Boolean) {
    super.addPlayerListener(this)
    if (internalPlayer == null) {
      playerPrepared = false
    }

    if (loadSource) ensurePlayer()
  }

  override fun ready() {
    ensurePlayer()
    renderer?.setupPlayer()
  }

  override fun play() {
    requireNotNull(internalPlayer).wrappedPlayer.play()
  }

  override fun pause() {
    if (playerPrepared) internalPlayer?.wrappedPlayer?.pause()
  }

  override fun reset() {
    (internalPlayer?.wrappedPlayer as? ExoPlayerWrapper)?.resetAdsBundle()
    playbackRestoreInfo = PlaybackInfo.EMPTY
    internalPlayer?.let { player ->
      player.stop()
      player.clearMediaItems()
    }
    playerPrepared = false
  }

  override fun release() {
    renderer?.player = null
    playbackRestoreInfo = PlaybackInfo.EMPTY
    internalPlayer?.let { player ->
      player.stop()
      player.clearMediaItems()
      playerPool.putPlayer(player = player.wrappedPlayer)
    }
    internalPlayer = null
    playerPrepared = false
    (internalPlayer?.wrappedPlayer as? ExoPlayerWrapper)?.releaseAdsBundle()
    super.removePlayerListener(this)
  }

  //region ErrorMessageProvider<Throwable>
  override fun getErrorMessage(throwable: Throwable): Pair<Int, String> {
    val cause: Throwable? = throwable.cause
    val errorString: String = if (cause is DecoderInitializationException) {
      // Special case for decoder initialization failures.
      val codecInfo = cause.codecInfo
      if (codecInfo == null) {
        when {
          cause.cause is DecoderQueryException ->
            appContext.getString(R.string.error_querying_decoders)
          cause.secureDecoderRequired ->
            appContext.getString(R.string.error_no_secure_decoder, cause.mimeType)
          else -> appContext.getString(R.string.error_no_decoder, cause.mimeType)
        }
      } else {
        appContext.getString(R.string.error_instantiating_decoder, codecInfo.name)
      }
    } else {
      appContext.getString(R.string.error_generic)
    }

    return Pair.create(0, errorString)
  }
  //endregion

  //region Player.Listener
  override fun onEvents(
    player: Player,
    events: Events
  ) {
    "Bridge[${hexCode()}]_STATS [stats=${playbackStatsListener.playbackStats}]".logInfo()
  }

  override fun onPlayerError(error: PlaybackException) {
    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
      internalPlayer?.let {
        it.seekToDefaultPosition()
        it.prepare()
      }
    }
  }

  override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
    if (tracksInfo != lastSeenTracksInfo) {
      if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_VIDEO)) {
        Toast.makeText(appContext, R.string.error_unsupported_video, Toast.LENGTH_LONG).show()
      }
      if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_AUDIO)) {
        Toast.makeText(appContext, R.string.error_unsupported_audio, Toast.LENGTH_LONG).show()
      }
      lastSeenTracksInfo = tracksInfo
    }
  }
  //endregion

  private fun ensurePlayer() {
    val player: Player = internalPlayer ?: run {
      playerPrepared = false
      val playerWrapper = InternalPlayerWrapper(player = playerPool.getPlayer(mediaItems))
      internalPlayer = playerWrapper
      playerWrapper
    }

    if (player.playbackState == Player.STATE_IDLE) {
      playerPrepared = false
    }

    val playbackInfo = playbackRestoreInfo
    playbackRestoreInfo = PlaybackInfo.EMPTY
    val hasStartPosition = playbackInfo.mediaItemIndex != C.INDEX_UNSET
    if (hasStartPosition) {
      player.seekTo(playbackInfo.mediaItemIndex, playbackInfo.currentPositionMillis)
    }

    if (!playerPrepared) {
      (internalPlayer?.wrappedPlayer as? ExoPlayerWrapper)?.prepareAdsBundle()
      player.setMediaItems(
        /* mediaItems */ mediaItems,
        /* resetPosition */ !hasStartPosition
      )
      player.prepare()
      playerPrepared = true
    }
  }

  //region Ads support.
  @VisibleForTesting
  internal fun ExoPlayerWrapper.prepareAdsBundle() {
    val hasAd = mediaItems.any { it.localConfiguration?.adsConfiguration != null }
    mediaItems.takeIf { hasAd } ?: return

    val imaBundle = ImaSetupBundle(
      // TODO: API for clients to use their own ImaAdsLoader.Builder.
      adsLoader = ImaAdsLoader.Builder(appContext)
        .setAdEventListener(adComponentsListeners)
        .setAdErrorListener(adComponentsListeners)
        .setVideoAdPlayerCallback(adComponentsListeners)
        .build(),
      adViewGroup = FrameLayout(appContext),
    )

    requireNotNull(mediaSourceFactory) {
      "To support MediaItem with Ad, client needs to use a DefaultMediaSourceFactory."
    }
      .apply {
        setAdsLoaderProvider(imaBundle)
        setAdViewProvider(imaBundle)
      }
    imaBundle.ready(this)
    imaSetupBundle = imaBundle
  }

  @VisibleForTesting
  internal fun ExoPlayerWrapper.resetAdsBundle() {
    mediaSourceFactory?.apply {
      setAdsLoaderProvider(null)
      setAdViewProvider(null)
    }
    imaSetupBundle?.reset()
  }

  @VisibleForTesting
  internal fun ExoPlayerWrapper.releaseAdsBundle() {
    mediaSourceFactory?.apply {
      setAdsLoaderProvider(null)
      setAdViewProvider(null)
    }
    imaSetupBundle?.release()
    imaSetupBundle = null
  }
  //endregion

  private fun StyledPlayerView.setupPlayer() {
    if (this.player !== internalPlayer) this.player = internalPlayer
  }

  private inner class InternalPlayerWrapper(val player: ExoPlayer) : ForwardingPlayer(player) {
    override fun play() = controller.play()
    override fun pause() = controller.pause()
    override fun getWrappedPlayer(): ExoPlayer = player
  }

  private companion object {

    fun getFriendlyObstructionPurpose(@Purpose purpose: Int): FriendlyObstructionPurpose {
      return when (purpose) {
        AdOverlayInfo.PURPOSE_CONTROLS -> FriendlyObstructionPurpose.VIDEO_CONTROLS
        AdOverlayInfo.PURPOSE_CLOSE_AD -> FriendlyObstructionPurpose.CLOSE_AD
        AdOverlayInfo.PURPOSE_NOT_VISIBLE -> FriendlyObstructionPurpose.NOT_VISIBLE
        AdOverlayInfo.PURPOSE_OTHER -> FriendlyObstructionPurpose.OTHER
        else -> FriendlyObstructionPurpose.OTHER
      }
    }
  }
}
