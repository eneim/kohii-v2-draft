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

import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.core.BasePlayable
import kohii.v2.core.Bridge
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableManager

/**
 * A [Playable] that uses [StyledPlayerView] as the renderer.
 */
internal class StyledPlayerViewPlayable(
  home: Home,
  tag: String,
  data: Any,
  firstManager: PlayableManager,
  bridge: Bridge<StyledPlayerView>,
) : BasePlayable<StyledPlayerView>(
  home = home,
  tag = tag,
  data = data,
  bridge = bridge,
  rendererType = StyledPlayerView::class.java,
  firstManager = firstManager
) {

  override fun onRendererAttached(renderer: Any?) {
    super.onRendererAttached(renderer)
    if (renderer != null) {
      require(renderer is StyledPlayerView) { "$renderer is not a StyledPlayerView." }
      bridge.renderer = renderer
    } else {
      bridge.renderer = null
    }
  }
}
