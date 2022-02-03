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

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v2.core.Engine
import kohii.v2.core.Home
import kohii.v2.core.Playback
import kohii.v2.core.RecycledRendererProvider
import kohii.v2.core.playbackManager
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.StyledPlayerViewProvider

class YouTubePlayerViewProvider(poolSize: Int = 2) : RecycledRendererProvider(poolSize) {

  override fun createRenderer(
    playback: Playback,
    rendererType: Int,
  ): Any {
    val container = playback.container as ViewGroup
    val iFramePlayerOptions = IFramePlayerOptions.Builder()
      .controls(0)
      .build()

    return YouTubePlayerView(context = container.context).apply {
      enableAutomaticInitialization = false
      enableBackgroundPlayback(false)
      initialize(
        youTubePlayerListener = object : AbstractYouTubePlayerListener() {},
        handleNetworkEvents = true,
        playerOptions = iFramePlayerOptions
      )
    }
  }

  override fun recycleRenderer(renderer: Any) = Unit

  override fun accept(playback: Playback): Boolean {
    return playback.container is ViewGroup
  }
}

/**
 * Creates a new [Engine] that supports the ExoPlayer stack.
 */
@Suppress("FunctionName")
fun Fragment.YouTubeEngine(bucket: View): Engine {
  val manager = playbackManager()
  manager.bucket(bucket)
  return Engine.get<YouTubePlayerView>(
    manager = manager,
    playableCreator = YouTubePlayerPlayable.Creator(Home[this]),
    rendererProvider = YouTubePlayerViewProvider(),
  )
}
