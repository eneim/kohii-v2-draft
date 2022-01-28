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

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.R
import kohii.v2.core.Playback
import kohii.v2.core.RecycledRendererProvider

class StyledPlayerViewProvider : RecycledRendererProvider() {

  override fun getRendererType(
    container: Any,
    data: Any,
  ): Int {
    val isDrmMedias = when (data) {
      is MediaItem -> data.isDrmMedia
      is Collection<*> -> data.any { it is MediaItem && it.isDrmMedia }
      else -> false
    }
    // Note: we want to use SurfaceView on API 24 and above. But reusing SurfaceView doesn't seem to
    // be straight forward, as it is not trivial to clean the cache of old video ...
    return if (isDrmMedias) {
      R.layout.exoplayer_styled_player_view_surface
    } else {
      R.layout.exoplayer_styled_player_view_texture
    }
  }

  override fun createRenderer(
    playback: Playback,
    rendererType: Int,
  ): Any {
    val container = playback.container as ViewGroup
    return LayoutInflater.from(container.context)
      .inflate(rendererType, container, false) as StyledPlayerView
  }

  override fun recycleRenderer(renderer: Any) {
    // View must be removed from its parent before this call.
    require(renderer is StyledPlayerView && renderer.parent == null && !renderer.isAttachedToWindow)
    renderer.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Default
    renderer.setAspectRatioListener(null)
    renderer.setControllerOnFullScreenModeChangedListener(null)
    renderer.setErrorMessageProvider(null)
    renderer.setControllerVisibilityListener(null)
    renderer.adViewGroup.removeAllViews()
  }

  override fun accept(playback: Playback): Boolean {
    return playback.container is ViewGroup
  }

  private companion object {
    val MediaItem.isDrmMedia: Boolean get() = localConfiguration?.drmConfiguration != null
  }
}
