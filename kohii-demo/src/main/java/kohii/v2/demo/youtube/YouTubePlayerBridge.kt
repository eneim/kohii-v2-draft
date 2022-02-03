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

package kohii.v2.demo.youtube

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.BUFFERING
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.ENDED
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PAUSED
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PLAYING
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.UNKNOWN
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.VIDEO_CUED
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v2.core.AbstractBridge
import kohii.v2.core.PlayableState
import kohii.v2.core.PlayableState.Ended
import kohii.v2.core.PlayableState.Progress
import java.util.concurrent.TimeUnit

// Use `mediaId` for the YouTube video Id.
class YouTubePlayerBridge(
  private val media: MediaItem,
) : AbstractBridge<YouTubePlayerView>() {

  private val tracker = InternalYouTubePlayerTracker()

  override val isStarted: Boolean
    get() = tracker.state == VIDEO_CUED

  override val isPlaying: Boolean
    get() = tracker.state == PLAYING

  override var renderer: YouTubePlayerView? = null
    set(value) {
      if (field === value) return
      if (value == null) player = null
      field = value
    }

  override var playableState: PlayableState = PlayableState.Initialized
    set(value) {
      field = value
      val seekSeconds = (value as? Progress)?.currentPositionSeconds ?: 0F
      player?.seekTo(seekSeconds)
    }

  private val playerListener = object : AbstractYouTubePlayerListener() {
    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerState,
    ) {
      tracker.state = state
      val playerState = state.toPlayerState()
      playerListeners.onPlaybackStateChanged(playerState)
    }

    override fun onError(
      youTubePlayer: YouTubePlayer,
      error: PlayerError,
    ) {
      // TODO: handle error.
    }
  }

  private var internalPlayableState: PlayableState = PlayableState.Initialized
  private var player: YouTubePlayer? = null
    set(value) {
      val from = field
      field = value
      val to = field
      if (from === to) return
      updatePlayableState(from)
      if (from != null) {
        from.removeListener(tracker)
        from.removeListener(playerListener)
      }
      if (to != null) {
        to.addListener(tracker)
        to.addListener(playerListener)
      }
    }

  private fun updatePlayableState(player: YouTubePlayer?) {
    internalPlayableState = PlayableState.Idle
  }

  override fun prepare(loadSource: Boolean) = Unit

  override fun ready() {
  }

  override fun play() {
    val player = this.player
    val videoId = media.mediaId
    val isEnded = internalPlayableState == Ended
    if (!isEnded && (tracker.state != PLAYING || tracker.videoId != videoId)) {
      val startSeconds = (internalPlayableState as? Progress)?.currentPositionSeconds ?: 0F
      if (tracker.videoId == videoId && player != null) {
        player.play()
        player.seekTo(startSeconds)
      } else {
        loadAndPlayVideo(videoId, startSeconds)
      }
    }
  }

  override fun pause() {
    updatePlayableState(player)
    player?.pause()
  }

  override fun reset() {
    internalPlayableState = PlayableState.Idle
    player?.pause()
  }

  override fun release() {
    updatePlayableState(player)
    player?.also {
      it.removeListener(tracker)
      it.removeListener(playerListener)
    }
  }

  private fun prepareVideo(
    videoId: String,
    startSeconds: Float,
  ) {
    player?.cueVideo(videoId, startSeconds) ?: run {
      val playerView = requireNotNull(renderer)
      val callback = object : DeferredCueVideoCallback(videoId, startSeconds) {
        override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
          this@YouTubePlayerBridge.player = youTubePlayer
          super.onYouTubePlayer(youTubePlayer)
        }
      }
      playerView.getYouTubePlayerWhenReady(callback)
    }
  }

  private fun loadAndPlayVideo(
    videoId: String,
    startSeconds: Float,
  ) {
    player?.loadVideo(videoId, startSeconds) ?: run {
      val playerView = requireNotNull(renderer)
      val callback = object : DeferredLoadVideoCallback(videoId, startSeconds) {
        override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
          this@YouTubePlayerBridge.player = youTubePlayer
          super.onYouTubePlayer(youTubePlayer)
        }
      }
      playerView.getYouTubePlayerWhenReady(callback)
    }
  }

  internal class InternalYouTubePlayerTracker : AbstractYouTubePlayerListener() {
    var state: PlayerState = UNKNOWN
    var currentPositionSeconds: Float = 0f
      private set
    var videoDurationSeconds: Float = 0f
      private set
    var videoId: String? = null
      private set

    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerState,
    ) {
      this.state = state
    }

    override fun onCurrentSecond(
      youTubePlayer: YouTubePlayer,
      second: Float,
    ) {
      currentPositionSeconds = second
    }

    override fun onVideoDuration(
      youTubePlayer: YouTubePlayer,
      duration: Float,
    ) {
      videoDurationSeconds = duration
    }

    override fun onVideoId(
      youTubePlayer: YouTubePlayer,
      videoId: String,
    ) {
      this.videoId = videoId
    }
  }

  internal open class DeferredLoadVideoCallback(
    private val videoId: String,
    private val startPos: Float,
  ) : YouTubePlayerCallback {
    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
      youTubePlayer.loadVideo(videoId, startPos)
    }
  }

  internal open class DeferredCueVideoCallback(
    private val videoId: String,
    private val startPos: Float,
  ) : YouTubePlayerCallback {
    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
      youTubePlayer.cueVideo(videoId, startPos)
    }
  }
}

@Player.State
private fun PlayerState.toPlayerState(): Int {
  return when (this) {
    VIDEO_CUED -> Player.STATE_READY
    PLAYING -> Player.STATE_READY
    BUFFERING -> Player.STATE_BUFFERING
    ENDED -> Player.STATE_ENDED
    PAUSED -> Player.STATE_READY
    else -> Player.STATE_IDLE
  }
}

private val Progress?.currentPositionSeconds: Float
  get() = TimeUnit.MILLISECONDS
    .toSeconds(this?.currentPositionMillis ?: 0)
    .toFloat()
