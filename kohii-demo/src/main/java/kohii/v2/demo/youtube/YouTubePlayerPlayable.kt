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

import android.util.Range
import com.google.android.exoplayer2.MediaItem
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v2.core.BasePlayable
import kohii.v2.core.Bridge
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableCreator
import kohii.v2.core.PlayableManager

class YouTubePlayerPlayable(
  home: Home,
  tag: String,
  data: Any,
  bridge: Bridge<YouTubePlayerView>,
  firstManager: PlayableManager,
) : BasePlayable<YouTubePlayerView>(
  home,
  tag,
  data,
  bridge,
  YouTubePlayerView::class.java,
  firstManager
) {

  override val triggerRange: Range<Float> = Range(0.999f, 1.0f)

  override fun onRendererAttached(renderer: Any?) {
    super.onRendererAttached(renderer)
    if (renderer != null) {
      require(renderer is YouTubePlayerView) { "$renderer is not a YouTubePlayerView." }
      bridge.renderer = renderer
    } else {
      bridge.renderer = null
    }
  }

  class Creator(
    private val home: Home,
  ) : PlayableCreator() {

    override fun createPlayable(
      playableManager: PlayableManager,
      data: Any,
      tag: String,
    ): Playable {
      val mediaItem = if (data is MediaItem) {
        data
      } else {
        MediaItem.Builder().setMediaId(data.toString()).build()
      }

      return YouTubePlayerPlayable(
        home = home,
        tag = tag,
        data = data,
        YouTubePlayerBridge(media = mediaItem),
        firstManager = playableManager,
      )
    }
  }
}
