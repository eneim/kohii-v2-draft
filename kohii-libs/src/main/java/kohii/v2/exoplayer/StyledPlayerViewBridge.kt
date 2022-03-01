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
import com.google.ads.interactivemedia.v3.api.FriendlyObstruction
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
import com.google.android.exoplayer2.parameters
import com.google.android.exoplayer2.ui.AdOverlayInfo
import com.google.android.exoplayer2.ui.AdOverlayInfo.Purpose
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import kohii.v2.R
import kohii.v2.core.AbstractBridge
import kohii.v2.core.Bridge
import kohii.v2.core.PlayableState
import kohii.v2.core.PlayableState.Active
import kohii.v2.core.PlayableState.Ended
import kohii.v2.core.PlayableState.Idle
import kohii.v2.core.PlayableState.Initialized
import kohii.v2.core.PlayerPool
import kohii.v2.core.PlayerProgress
import kohii.v2.internal.doOnTrackInfoChanged
import kohii.v2.internal.hexCode
import kohii.v2.internal.logInfo

/**
 * A [Bridge] that works with [StyledPlayerView] and [ExoPlayer].
 *
 * Note: [ExoPlayer] is required rather than [Player] for Ad support and AnalyticsListener usage.
 * This [Bridge] supports playing a list of [MediaItem] with ads that can be played by a single
 * [ImaAdsLoader].
 */
// TODO: when a video is paused manually, do not start the next one automatically :thinking:
internal class StyledPlayerViewBridge(
  context: Context,
  private val mediaItems: List<MediaItem>,
  private val playerPool: PlayerPool<ExoPlayer>,
) : AbstractBridge<StyledPlayerView>(), Player.Listener, ErrorMessageProvider<Throwable> {

  private val appContext: Context = context.applicationContext
  private val playbackStatsListener = PlaybackStatsListener(false, null)
  private val playerListener = ComponentsListener(playerListener = this)

  private var imaSetupBundle: ImaSetupBundle? = null
  private var internalPlayer: InternalExoPlayerWrapper? = null
    set(value) {
      if (field !== value) {
        field?.wrappedPlayer?.removeAnalyticsListener(playbackStatsListener)
        field?.wrappedPlayer?.removeListener(componentsListeners)
        field = value
        field?.wrappedPlayer?.addListener(componentsListeners)
        field?.wrappedPlayer?.addAnalyticsListener(playbackStatsListener)
      }
    }

  @get:Player.State
  private var lastSeenPlayerState: Int = Player.STATE_IDLE
  private var lastSeenTracksInfo: TracksInfo = TracksInfo.EMPTY
  private var playerPrepared = false

  // A temporary data for restoration only. It will be cleared after the restoration.
  private var lastSeenState: Active = Active.DEFAULT

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
            val imaSdkFactory = ImaSdkFactory.getInstance()
            for (adOverlayInfo in value.adOverlayInfos) {
              container.registerFriendlyObstruction(
                imaSdkFactory.createFriendlyObstruction(adOverlayInfo)
              )
            }
          }
        }
      }
      field = value
    }

  override var playableState: PlayableState = Initialized
    get() {
      if (lastSeenPlayerState == Player.STATE_ENDED) return Ended
      val player = internalPlayer
      return when (val playerState: Int = player?.playbackState ?: lastSeenPlayerState) {
        Player.STATE_ENDED -> Ended
        Player.STATE_IDLE -> Idle
        else -> if (player != null) {
          Active(
            progress = PlayerProgress(
              isStarted = isStarted,
              totalDurationMillis = player.duration,
              contentDurationMillis = player.contentDuration,
              currentPositionMillis = player.currentPosition,
              currentMediaItemIndex = player.currentMediaItemIndex,
            ),
            extras = ExoPlayerExtras(
              playerState = playerState,
              playerParameters = player.player.parameters,
              trackSelectionParameters = player.trackSelectionParameters,
            ),
          )
        } else {
          Idle
        }
      }
    }
    set(value) {
      field = value
      lastSeenPlayerState = when (value) {
        Ended -> Player.STATE_ENDED
        Initialized, Idle -> Player.STATE_IDLE
        is Active -> (value.extras as? ExoPlayerExtras)?.playerState ?: Player.STATE_IDLE
      }

      if (value is Active) {
        val playerProgress = value.progress
        val playerState = value.extras as? ExoPlayerExtras ?: ExoPlayerExtras.DEFAULT
        val player: InternalExoPlayerWrapper? = internalPlayer
        if (player != null) {
          if (playerProgress.currentMediaItemIndex != C.INDEX_UNSET) {
            player.seekTo(
              playerProgress.currentMediaItemIndex,
              playerProgress.currentPositionMillis
            )
          }

          player.player.parameters = playerState.playerParameters
          if (lastSeenTracksInfo !== TracksInfo.EMPTY) {
            player.trackSelectionParameters = playerState.trackSelectionParameters
          } else {
            player.doOnTrackInfoChanged {
              player.trackSelectionParameters = playerState.trackSelectionParameters
            }
          }
          lastSeenState = Active.DEFAULT
        } else {
          lastSeenState = value
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
    super.addComponentsListener(playerListener)
    if (internalPlayer == null) {
      playerPrepared = false
    }

    if (loadSource) preparePlayer()
  }

  override fun ready() {
    preparePlayer()
    renderer?.player = internalPlayer
  }

  override fun play() {
    // It can be skip if the Player/Playback has ended before.
    if (playerPrepared) {
      requireNotNull(internalPlayer).wrappedPlayer.play()
    }
  }

  override fun pause() {
    if (playerPrepared) internalPlayer?.wrappedPlayer?.pause()
  }

  override fun reset() {
    (internalPlayer?.wrappedPlayer as? ExoPlayerWrapper)?.resetAdsBundle()
    lastSeenState = Active.DEFAULT
    internalPlayer?.let { player ->
      player.stop()
      player.clearMediaItems()
    }
    // Note: playerPrepared is set to false, but the internalPlayer can be nonnull.
    playerPrepared = false
    lastSeenPlayerState = Player.STATE_IDLE
  }

  // This method should not reset lastSeenPlayerState because it may be called when
  // the container is scrolled off-screen and we need to release the resource hold by inactive
  // Playable. When it is scrolled back, the Playable will be prepared again.
  override fun release() {
    renderer?.player = null
    lastSeenState = Active.DEFAULT
    internalPlayer?.let { player ->
      player.stop()
      player.clearMediaItems()
      (player.wrappedPlayer as? ExoPlayerWrapper)?.releaseAdsBundle()
      playerPool.putPlayer(player = player.wrappedPlayer)
    }
    internalPlayer = null
    playerPrepared = false
    super.removeComponentsListener(playerListener)
    super.release()
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
    events: Events,
  ) {
    "Bridge[${hexCode()}]_STATS [stats=${playbackStatsListener.playbackStats}]".logInfo()
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    lastSeenPlayerState = playbackState
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

  // Make sure the Player instance is available and set the MediaItems.
  private fun initPlayer(): InternalExoPlayerWrapper {
    val player = internalPlayer ?: run {
      playerPrepared = false
      InternalExoPlayerWrapper(player = playerPool.getPlayer(mediaItems))
    }

    val activeState = lastSeenState
    val playerExtras = activeState.extras as? ExoPlayerExtras ?: ExoPlayerExtras.DEFAULT
    lastSeenState = Active.DEFAULT

    if (playerExtras !== ExoPlayerExtras.DEFAULT) {
      player.player.parameters = playerExtras.playerParameters
      player.doOnTrackInfoChanged {
        trackSelectionParameters = playerExtras.trackSelectionParameters
      }
    }

    val hasStartPosition = activeState.progress.currentMediaItemIndex != C.INDEX_UNSET
    if (hasStartPosition) {
      player.seekTo(
        activeState.progress.currentMediaItemIndex,
        activeState.progress.currentPositionMillis,
      )
    }

    if (!playerPrepared) {
      (player.wrappedPlayer as? ExoPlayerWrapper)?.prepareAdsBundle()
      // After the next step, the MediaSource.Factory will also create AdsMediaSource if needed,
      // and update the ImaAdsLoader.
      player.setMediaItems(
        /* mediaItems */ mediaItems,
        /* resetPosition */ !hasStartPosition
      )
    }

    return player
  }

  // Make sure that the Player is available and prepared.
  // Note: this method will do nothing if `lastSeenPlayerState` is Player.STATE_ENDED
  private fun preparePlayer() {
    val player = initPlayer()
    internalPlayer = player

    if (lastSeenPlayerState == Player.STATE_ENDED) return
    if (player.playbackState == Player.STATE_IDLE) playerPrepared = false

    if (!playerPrepared) {
      player.prepare()
      playerPrepared = true
    }
  }

  //region Ads support.
  @VisibleForTesting
  internal fun ExoPlayerWrapper.prepareAdsBundle() {
    if (!mediaItems.hasAd) return

    val imaBundle = ImaSetupBundle(
      // TODO: API for clients to use their own ImaAdsLoader.Builder.
      adsLoader = ImaAdsLoader.Builder(appContext)
        .setAdEventListener(componentsListeners)
        .setAdErrorListener(componentsListeners)
        .setVideoAdPlayerCallback(componentsListeners)
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

  // When this player is set to the [StyledPlayerView] and then its [StyledPlayerControlView], it
  // will redirect the call to [play()] and [pause()] to the `controller`. This is to centralize the
  // manual play and pause request to the Playback:
  // - A call to [play()] will set a special flag to the [Playable], indicate that the Playback is
  // started by the user, and then it will refresh to send the request to this Bridge to start the
  // actual Player (if the Playback is in the condition to be able to start).
  // - Similarly, a call to [pause()] will set a special flag to the [Playable], indicate that the
  // Playback is paused by the user, and then it will refresh to send the pause request to this
  // Bridge to pause the actual Player.
  private inner class InternalExoPlayerWrapper(val player: ExoPlayer) : ForwardingPlayer(player) {
    override fun play() = controller.play()
    override fun pause() = controller.pause()
    override fun getWrappedPlayer(): ExoPlayer = player // Typed overriding.
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

    fun ImaSdkFactory.createFriendlyObstruction(adOverlayInfo: AdOverlayInfo): FriendlyObstruction =
      createFriendlyObstruction(
        adOverlayInfo.view,
        getFriendlyObstructionPurpose(adOverlayInfo.purpose),
        adOverlayInfo.reasonDetail
      )
  }
}

private val List<MediaItem>.hasAd: Boolean
  get() = this.any {
    it.localConfiguration?.adsConfiguration != null
  }
