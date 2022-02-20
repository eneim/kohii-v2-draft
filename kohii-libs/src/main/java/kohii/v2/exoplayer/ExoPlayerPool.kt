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
import com.google.android.exoplayer2.ExoPlayerWrapper
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.parameters
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import kohii.v2.core.PlayerPool

/**
 * A [PlayerPool] that manages instances of [Player]s.
 */
class ExoPlayerPool(
  context: Context,
  private val builder: ExoPlayer.Builder.() -> Unit = {},
) : PlayerPool<ExoPlayer>() {

  private val app: Context = context.applicationContext

  override fun ExoPlayer.accept(mediaData: Any): Boolean {
    return mediaData is MediaItem ||
      (mediaData is Collection<*> && mediaData.all { it is MediaItem })
  }

  override fun createPlayer(mediaData: Any): ExoPlayer =
    ExoPlayerWrapper(ExoPlayer.Builder(app).apply(builder))

  override fun resetPlayer(player: ExoPlayer) {
    player.stop()
    player.clearMediaItems()
    player.parameters = PlayerParameters.DEFAULT
    player.trackSelectionParameters = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
  }

  override fun destroyPlayer(player: ExoPlayer) {
    player.release()
  }
}
