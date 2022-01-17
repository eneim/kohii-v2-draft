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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kohii.v2.core.Home
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.PlayableKey
import kohii.v2.core.Playback
import kohii.v2.core.Playback.Config

internal class BindRequest(
  val home: Home,
  val manager: Manager,
  val playableKey: PlayableKey, // Tag used by Home to store the Playable.
  val container: Any,
  val payload: Lazy<Playable>,
  val config: Config,
) {

  internal val lifecycle: Lifecycle = manager.lifecycleOwner.lifecycle

  @Throws(IllegalArgumentException::class)
  internal suspend fun onBind(): Playback {
    "Home_Request[${hexCode()}]_BIND_Begin".logDebug()
    if (container is View) {
      container.awaitAttached()
    } else {
      // Not yet implemented to support LifecycleOwner. The creation of the BindRequest guards this.
      requireNotNull(container as LifecycleOwner).lifecycle.awaitStarted()
    }

    // TODO: check the order for the case multiple Buckets can accept the container?
    val bucket = manager.requireBucket(container)
    val playable = payload.value
    home.cancelPlayableDestruction(playable)
    home.playables[playable] = playableKey

    val createNewPlayback: () -> Playback = {
      manager
        .requirePlaybackCreator(playable, container)
        .createPlayback(
          bucket = bucket,
          playable = playable,
          container = container,
          config = config,
        )
    }

    val sameContainer: Playback? = manager.playbacks[container]
    val samePlayable: Playback? = playable.playback

    "Home_Request[${hexCode()}]_BIND: SC=$sameContainer, SP=$samePlayable".logDebug()

    val boundPlayback: Playback = BindingType
      .get(playable, samePlayable, sameContainer)
      .performBind(createNewPlayback)

    manager.refresh() // Kick it.
    "Request[${hexCode()}]_BIND_End result=$boundPlayback".logDebug()
    return boundPlayback
  }

  override fun toString(): String = "R@${hexCode()}"
}
