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

/**
 * A [Playback] where its [container] is also the renderer.
 */
internal class StaticViewRendererPlayback(
  playable: Playable,
  bucket: Bucket,
  manager: Manager,
  container: View,
  tag: String,
  config: Config,
) : ViewPlayback(
  playable = playable,
  bucket = bucket,
  manager = manager,
  viewContainer = container,
  tag = tag,
  config = config,
) {

  override fun onActivate() {
    super.onActivate()
    playable.onRendererAttached(container)
  }

  override fun onDeactivate() {
    playable.onRendererDetached(container)
    super.onDeactivate()
  }

  override fun detachRenderer() = Unit
}
