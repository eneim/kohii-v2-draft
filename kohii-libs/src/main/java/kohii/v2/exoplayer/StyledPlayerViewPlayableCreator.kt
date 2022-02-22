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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.common.Capsule
import kohii.v2.core.Engine
import kohii.v2.core.Home
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.PlayableCreator
import kohii.v2.core.PlayableManager
import kohii.v2.core.PlayerPool
import kotlin.LazyThreadSafetyMode.NONE

/**
 * A [PlayableCreator] that supports [ExoPlayer] as playback backend, and [StyledPlayerView] as the
 * playback frontend. Instance of this class is used to initialize the [Engine] object. Resource of
 * this class will be cleared by [onClear] automatically when there is no active [Manager] that is
 * using it.
 *
 * Using [StyledPlayerViewPlayableCreator.getInstance] to obtain an instance of this class.
 */
class StyledPlayerViewPlayableCreator private constructor(
  private val home: Home,
  private val playerPool: Lazy<PlayerPool<ExoPlayer>>,
) : PlayableCreator() {

  private constructor(context: Context) : this(
    home = Home[context.applicationContext],
    playerPool = lazy(NONE) { ExoPlayerPool(context.applicationContext) }
  )

  override fun createPlayable(
    playableManager: PlayableManager,
    data: List<MediaItem>,
    tag: String,
  ): Playable = StyledPlayerViewPlayable(
    home = home,
    tag = tag,
    data = data,
    firstManager = playableManager,
    bridge = StyledPlayerViewBridge(
      context = home.application,
      mediaItems = data,
      playerPool = playerPool.value
    )
  )

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
