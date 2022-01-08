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
import com.google.android.exoplayer2.Player
import kohii.v2.core.PlayerPool

/**
 * A [PlayerPool] that manages instances of [Player]s.
 */
class ExoPlayerPool(
  context: Context,
  private val builder: ExoPlayer.Builder.() -> Unit = {},
) : PlayerPool<Player>() {

  private val app: Context = context.applicationContext

  override fun Player.accept(mediaData: Any): Boolean {
    return mediaData is MediaItem ||
      (mediaData is Collection<*> && mediaData.all { it is MediaItem })
  }

  override fun createPlayer(mediaData: Any): Player = ExoPlayer.Builder(app)
    .apply(builder)
    .build()

  override fun resetPlayer(player: Player) {
    player.stop()
    player.clearMediaItems()
  }

  override fun destroyPlayer(player: Player) {
    player.release()
  }
}
