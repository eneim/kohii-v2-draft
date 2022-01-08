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
import android.view.ViewGroup
import kohii.v2.core.Bucket
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.Playback
import kohii.v2.core.Playback.Config
import kohii.v2.core.PlaybackCreator
import kohii.v2.core.RendererProviderManager

internal class DynamicViewPlaybackCreator(
  manager: Manager,
  private val rendererProviderManager: RendererProviderManager,
) : PlaybackCreator(manager) {

  override fun accept(
    playable: Playable,
    container: Any
  ): Boolean = container is ViewGroup &&
      !playable.rendererType.isAssignableFrom(container.javaClass) &&
      View::class.java.isAssignableFrom(playable.rendererType)

  override fun createPlayback(
    bucket: Bucket,
    playable: Playable,
    container: Any,
    config: Config,
  ): Playback = DynamicViewRendererPlayback(
    playable = playable,
    bucket = bucket,
    manager = bucket.manager,
    container = container as ViewGroup,
    providerManager = rendererProviderManager,
    tag = playable.tag,
    config = config,
  )
}
