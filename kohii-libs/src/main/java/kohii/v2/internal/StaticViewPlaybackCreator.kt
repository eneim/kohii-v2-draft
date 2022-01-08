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

package kohii.v2.internal

import android.view.View
import kohii.v2.core.Bucket
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.Playback
import kohii.v2.core.Playback.Config
import kohii.v2.core.PlaybackCreator

internal class StaticViewPlaybackCreator(manager: Manager) : PlaybackCreator(manager) {

  override fun accept(
    playable: Playable,
    container: Any
  ): Boolean =
    container is View && playable.rendererType.isAssignableFrom(container.javaClass)

  override fun createPlayback(
    bucket: Bucket,
    playable: Playable,
    container: Any,
    config: Config
  ): Playback = StaticViewRendererPlayback(
    playable = playable,
    bucket = bucket,
    manager = manager,
    container = container as View,
    tag = playable.tag,
    config = config,
  )
}
