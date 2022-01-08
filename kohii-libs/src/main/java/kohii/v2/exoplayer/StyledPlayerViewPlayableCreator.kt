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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kohii.v2.common.Capsule
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableCreator
import kohii.v2.core.PlayableManager
import kohii.v2.core.PlayerPool
import kotlin.LazyThreadSafetyMode.NONE

class StyledPlayerViewPlayableCreator private constructor(
  private val home: Home,
  private val playerPool: Lazy<PlayerPool<Player>>
) : PlayableCreator() {

  private constructor(context: Context) : this(
    home = Home[context.applicationContext],
    playerPool = lazy(NONE) { ExoPlayerPool(context.applicationContext) }
  )

  override fun createPlayable(
    playableManager: PlayableManager,
    data: Any,
    tag: String,
  ): Playable {
    val mediaItems: List<MediaItem> = when (data) {
      is Collection<*> -> data.filterIsInstance<MediaItem>()
        .takeIf { it.size == data.size }
        ?: throw IllegalArgumentException("$data is a collection that contain non-MediaItem item.")
      is MediaItem -> listOf(data) // Force cast.
      else -> listOf(MediaItem.fromUri(data.toString()))
    }

    return StyledPlayerViewPlayable(
      home = home,
      tag = tag,
      data = data,
      firstManager = playableManager,
      bridge = StyledPlayerViewBridge(
        context = home.application,
        mediaItems = mediaItems,
        playerPool = playerPool.value
      )
    )
  }

  override fun onClear() {
    super.onClear()
    if (playerPool.isInitialized()) {
      playerPool.value.clear()
    }
  }

  companion object {

    private val singleton = Capsule<PlayableCreator, Context>(::StyledPlayerViewPlayableCreator)

    fun getInstance(context: Context): PlayableCreator = singleton.get(context.applicationContext)
  }
}
