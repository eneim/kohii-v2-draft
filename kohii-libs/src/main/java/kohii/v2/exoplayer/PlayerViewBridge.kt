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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.media3.common.AdOverlayInfo
import androidx.media3.common.AdOverlayInfo.Purpose
import androidx.media3.common.AdViewProvider
import androidx.media3.common.C
import androidx.media3.common.ErrorMessageProvider
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Events
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlayerWrapper
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil.DecoderQueryException
import androidx.media3.exoplayer.parameters
import androidx.media3.exoplayer.source.ads.AdsLoader
import androidx.media3.ui.PlayerView
import com.google.ads.interactivemedia.v3.api.FriendlyObstruction
import com.google.ads.interactivemedia.v3.api.FriendlyObstructionPurpose
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
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
import kohii.v2.internal.doOnTracksChanged

/**
 * A [Bridge] that works with [PlayerView] and [ExoPlayer].
 *
 * Note: [ExoPlayer] is required rather than [Player] for Ad support and AnalyticsListener usage.
 * This [Bridge] supports playing a list of [MediaItem] with ads that can be played by a single
 * [ImaAdsLoader].
 */
// TODO: when a video is paused manually, do not start the next one automatically :thinking:
@Suppress("TooManyFunctions", "ForbiddenComment")
@UnstableApi
internal class PlayerViewBridge(
  context: Context,
  private val mediaItems: List<MediaItem>,
  private val playerPool: PlayerPool<ExoPlayer>,
) : AbstractBridge<PlayerView>(), Player.Listener, ErrorMessageProvider<Throwable> {

  private val appContext: Context = context.applicationContext
  private val playbackStatsListener = PlaybackStatsListener(false, null)
  private val playerListener = ComponentsListener(playerListener = this)

  private var imaSetupBundle: ImaSetupBundle? = null
  private var internalPlayer: InternalExoPlayerWrapper? = null
    set(value) {
      val previous = field
      if (previous !== value) {
        previous?.wrappedPlayer?.removeAnalyticsListener(playbackStatsListener)
        previous?.wrappedPlayer?.removeListener(componentsListeners)
        field = value
        value?.wrappedPlayer?.addListener(componentsListeners)
        value?.wrappedPlayer?.addAnalyticsListener(playbackStatsListener)
      }
    }

  @get:Player.State
  private var lastSeenPlayerState: Int = Player.STATE_IDLE
  private var lastSeenTracks: Tracks = Tracks.EMPTY
  private var playerPrepared = false

  // A temporary data for restoration only. It will be reset after the restoration.
  private var lastSeenPlayableState: Active = Active.DEFAULT

  override var renderer: PlayerView? = null
    set(value) {
      val previous = field
      if (previous === value) return

      val imaBundle = imaSetupBundle

      previous?.let { playerView ->
        playerView.setErrorMessageProvider(null)
        playerView.setCustomErrorMessage(null)
        if (imaBundle != null) {
          playerView.adViewGroup.removeView(imaBundle.adViewGroup)
          imaBundle.adsLoader.adDisplayContainer?.unregisterAllFriendlyObstructions()
        }
      }

      internalPlayer?.let { player ->
        PlayerView.switchTargetView(player, previous, value)
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
          if (lastSeenTracks !== Tracks.EMPTY) {
            player.trackSelectionParameters = playerState.trackSelectionParameters
          } else {
            player.doOnTracksChanged {
              player.trackSelectionParameters = playerState.trackSelectionParameters
            }
          }
          lastSeenPlayableState = Active.DEFAULT
        } else {
          lastSeenPlayableState = value
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
    get() = internalPlayer?.isPlaying ?: false

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
    lastSeenPlayableState = Active.DEFAULT
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
    lastSeenPlayableState = Active.DEFAULT
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
  ) = Unit

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

  override fun onTracksChanged(tracks: Tracks) {
    if (tracks != lastSeenTracks) {
      if (tracks.containsType(C.TRACK_TYPE_VIDEO) &&
        !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO, /* allowExceedsCapabilities = */ true)
      ) {
        Toast.makeText(appContext, R.string.error_unsupported_video, Toast.LENGTH_LONG).show()
      }
      if (tracks.containsType(C.TRACK_TYPE_AUDIO) &&
        !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO, /* allowExceedsCapabilities = */ true)
      ) {
        Toast.makeText(appContext, R.string.error_unsupported_audio, Toast.LENGTH_LONG).show()
      }
      lastSeenTracks = tracks
    }
  }
  //endregion

  // Make sure the Player instance is available and set with the MediaItems.
  private fun initPlayer(): InternalExoPlayerWrapper {
    val player = internalPlayer ?: run {
      playerPrepared = false
      InternalExoPlayerWrapper(player = playerPool.getPlayer(mediaItems))
    }

    val activeState = lastSeenPlayableState
    val playerExtras = activeState.extras as? ExoPlayerExtras ?: ExoPlayerExtras.DEFAULT
    lastSeenPlayableState = Active.DEFAULT

    if (playerExtras !== ExoPlayerExtras.DEFAULT) {
      player.player.parameters = playerExtras.playerParameters
      player.doOnTracksChanged {
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

    // FIXME: should call this check before initPlayer()?
    if (lastSeenPlayerState == Player.STATE_ENDED) return
    if (player.playbackState == Player.STATE_IDLE) {
      playerPrepared = false
    }

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
      adViewGroup = FrameLayout(appContext), // This will not be recreated on config change.
    )

    requireNotNull(mediaSourceFactory) {
      "To support MediaItem with Ad, client needs to use a DefaultMediaSourceFactory."
    }
      .setLocalAdInsertionComponents(
        /* adsLoaderProvider = */ imaBundle,
        /* adViewProvider = */ imaBundle
      )
    imaBundle.ready(this)
    imaSetupBundle = imaBundle
  }

  @VisibleForTesting
  internal fun ExoPlayerWrapper.resetAdsBundle() {
    mediaSourceFactory?.clearLocalAdInsertionComponents()
    imaSetupBundle?.reset()
  }

  @VisibleForTesting
  internal fun ExoPlayerWrapper.releaseAdsBundle() {
    mediaSourceFactory?.clearLocalAdInsertionComponents()
    imaSetupBundle?.release()
    imaSetupBundle = null
  }
  //endregion

  // When this player is set to the [PlayerView] and then its [PlayerControlView], it
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
    override fun getWrappedPlayer(): ExoPlayer = player
  }

  /**
   * A helper class used by the [PlayerViewBridge] to setup ads playback.
   *
   * This class uses a prebuilt [AdsLoader] but doesn't use the provided [AdsConfiguration] to
   * create the [AdsLoader] instance.
   */
  private class ImaSetupBundle(
    val adsLoader: ImaAdsLoader,
    private val adViewGroup: FrameLayout,
  ) : AdsLoader.Provider, AdViewProvider {

    //region AdsLoader.Provider
    override fun getAdsLoader(adsConfiguration: AdsConfiguration): AdsLoader = adsLoader
    //endregion

    //region AdViewProvider
    override fun getAdViewGroup(): ViewGroup = adViewGroup
    override fun getAdOverlayInfos(): MutableList<AdOverlayInfo> = mutableListOf()
    //endregion

    // Must be called before player.prepare()
    fun ready(player: Player) {
      adsLoader.setPlayer(player)
    }

    fun reset() {
      adsLoader.setPlayer(null)
    }

    fun release() {
      adsLoader.setPlayer(null)
      adsLoader.release()
    }
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
