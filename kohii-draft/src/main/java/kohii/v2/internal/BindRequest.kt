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

import android.view.View
import androidx.lifecycle.Lifecycle
import kohii.v2.common.logDebug
import kohii.v2.common.logError
import kohii.v2.core.Bucket
import kohii.v2.core.Home
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.Playback
import kohii.v2.core.Playback.Config

internal class BindRequest(
  val home: Home,
  val manager: Manager,
  val bucket: Bucket,
  val tag: Any,
  val container: Any, // Tag used by Home to store the Playable.
  val playable: Playable,
  val config: Config
) {

  internal val lifecycle: Lifecycle = manager.lifecycleOwner.lifecycle

  // TODO(eneim): Handle request cancelling when multiple binding is queued for the same container.
  internal suspend fun onBind(): Result<Playback> {
    "Request[${hexCode()}]_BIND_Begin".logDebug()
    if (container !is View) {
      val error = IllegalArgumentException("Non View container $container is not supported.")
      "Request[${hexCode()}]_BIND_End result=$error".logError()
      return Result.failure(error)
    }

    container.awaitAttached()
    if (!bucket.accepts(container)) {
      val error = IllegalArgumentException("Bucket $bucket doesn't accept container $container.")
      "Request[${hexCode()}]_BIND_End result=$error".logError()
      return Result.failure(error)
    }

    home.keepPlayable(playable)
    home.playables[playable] = tag

    val createNewPlayback = {
      manager
        .requirePlaybackCreator(playable, container)
        .createPlayback(
          bucket = bucket,
          playable = playable,
          container = container
        )
    }

    val sameContainer: Playback? = manager.playbacks[container]
    val samePlayable: Playback? = playable.playback

    val bindResult: Result<Playback> = //
      if (sameContainer == null) { // Bind to a clean Container
        if (samePlayable == null) {
          // Both sameContainer and samePlayable are null --> completely new binding
          val newPlayback = createNewPlayback()
          playable.internalPlayback = newPlayback
          manager.addPlayback(newPlayback)
          Result.success(newPlayback)
        } else {
          // samePlayable is not null -> a bound Playable to be rebound to a clean Container
          // Action: create new Playback for the Container, establish the new binding and remove
          // the old one of the 'samePlayable' Playback
          samePlayable.manager.removePlayback(samePlayable)
          val newPlayback = createNewPlayback()
          playable.internalPlayback = newPlayback
          manager.addPlayback(newPlayback)
          Result.success(newPlayback)
        }
      } else { // Bind to a Container that was bound to a different Playback previously.
        if (samePlayable == null) {
          // sameContainer is not null but samePlayable is null --> new Playable is bound to a bound Container
          // Action: create new Playback for current Container, make the new binding and remove old binding of
          // the 'sameContainer'
          sameContainer.manager.removePlayback(sameContainer)
          val newPlayback = createNewPlayback()
          playable.internalPlayback = newPlayback
          manager.addPlayback(newPlayback)
          Result.success(newPlayback)
        } else {
          // both sameContainer and samePlayable are not null --> a bound Playable to be rebound to
          // a bound Container
          if (sameContainer === samePlayable) {
            // Those Playbacks are the same -> Nothing to do.
            Result.success(samePlayable)
          } else {
            // Scenario: rebind a bound Playable from one Container to other Container that is being bound.
            // Action: remove both 'sameContainer' and 'samePlayable', create new one for the Container.
            // to the Container
            sameContainer.manager.removePlayback(sameContainer)
            samePlayable.manager.removePlayback(samePlayable, clearPlayable = false)
            val newPlayback = createNewPlayback()
            playable.internalPlayback = newPlayback
            manager.addPlayback(newPlayback)
            Result.success(newPlayback)
          }
        }
      }

    "Request[${hexCode()}]_BIND_End result=$bindResult".logDebug()
    return bindResult
  }

  override fun toString(): String = "R@${hexCode()}"
}
