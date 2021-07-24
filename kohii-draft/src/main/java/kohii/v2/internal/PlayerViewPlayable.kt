/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v2.internal

import android.widget.TextView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v2.common.logInfo
import kohii.v2.common.logWarn
import kohii.v2.core.Playable

class PlayerViewPlayable(
  tag: String,
  data: Any,
  val mediaItems: Collection<MediaItem>,
  rendererType: Class<PlayerView>
) : Playable(
  tag,
  data,
  rendererType
) {

  // FIXME(eneim): fix it once Bridge is introduced.
  override val isPlaying: Boolean get() = internalRenderer != null

  private var internalRenderer: TextView? = null

  override val renderer: Any? get() = internalRenderer

  override fun onStart() {
    super.onStart()
    internalRenderer?.text = "Started: $tag"
  }

  override fun onPause() {
    internalRenderer?.text = "Paused: $tag"
    super.onPause()
  }

  override fun onRelease() {
    "Playable[${hexCode()}]_RELEASE".logInfo()
  }

  override fun onRendererAttached(renderer: Any?) {
    "Playable[${hexCode()}]_ATTACH_Renderer [RR=${renderer?.asString()}]".logWarn()
    internalRenderer = renderer as? TextView
  }

  override fun onRendererDetached(renderer: Any?) {
    "Playable[${hexCode()}]_DETACH_Renderer [RR=${renderer?.asString()}]".logWarn()
    if (renderer === internalRenderer) {
      internalRenderer = null
    }
  }

  override fun toString(): String =
    "PB[${hexCode()}, ${rendererType.simpleName}, t=$tag, d=$mediaItems]"
}
