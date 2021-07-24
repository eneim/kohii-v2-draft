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

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v2.core.Playable
import kohii.v2.core.PlayableCreator
import kohii.v2.core.PlayableManager

internal class PlayerViewPlayableCreator : PlayableCreator {

  override fun accepts(mediaData: Any): Boolean {
    return mediaData is MediaItem || (mediaData is List<*> && mediaData.all { it is MediaItem })
  }

  override fun createPlayable(playableManager: PlayableManager, data: Any, tag: String): Playable {
    val mediaData: Collection<MediaItem> = when (data) {
      is MediaItem -> listOf(data)
      is List<*> -> data.filterIsInstance<MediaItem>()
      else -> throw IllegalArgumentException("$data is not supported by this class.")
    }

    val playable = PlayerViewPlayable(
      tag = tag,
      data = data,
      mediaItems = mediaData,
      rendererType = PlayerView::class.java
    )
    playable.manager = playableManager
    return playable
  }

  override fun cleanUp() {
    TODO("Not yet implemented")
  }
}
