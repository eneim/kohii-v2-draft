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
import kohii.v2.core.Binder.Payload
import kohii.v2.core.Home
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.PlayableKey
import kohii.v2.core.PlayableState
import kohii.v2.core.Playback
import kohii.v2.core.Playback.Config

internal class BindRequest(
  val home: Home,
  val manager: Manager,
  val playableKey: PlayableKey, // Tag used by Home to store the Playable.
  val container: Any,
  val payload: Lazy<Payload>,
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
    val payload = payload.value
    val playable = payload.playable
    home.cancelPlayableDestruction(playable)
    home.playables[playable] = playableKey

    val createNewPlayback = {
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

    val boundPlayback: Playback = //
      if (sameContainer == null) { // Bind to a clean Container
        if (samePlayable == null) {
          // Both sameContainer and samePlayable are null --> completely new binding
          val newPlayback = createNewPlayback()
          newPlayback.bindPlayable(playable, payload.state)
          newPlayback
        } else {
          // samePlayable is not null -> a bound Playable to be rebound to a clean Container
          // Action: create new Playback for the Container, establish the new binding and remove
          // the old one of the 'samePlayable' Playback
          // FIXME(eneim): why not calling this after `playable.playback` setter?
          samePlayable.manager.removePlayback(samePlayable, clearPlayable = false)
          val newPlayback = createNewPlayback()
          newPlayback.bindPlayable(playable, payload.state)
          newPlayback
        }
      } else { // Bind to a Container that was bound to a different Playback previously.
        if (samePlayable == null) {
          // Scenario: sameContainer is not null but samePlayable is null --> new Playable is bound
          // to a bound Container.
          // Action: create new Playback for current Container, make the new binding and remove old
          // binding of the 'sameContainer'
          // FIXME(eneim): why not calling this after `playable.playback` setter?
          sameContainer.manager.removePlayback(sameContainer)
          val newPlayback = createNewPlayback()
          newPlayback.bindPlayable(playable, payload.state)
          newPlayback
        } else {
          // Scenario: both sameContainer and samePlayable are not null --> a bound Playable to be
          // rebound to a bound Container
          if (sameContainer === samePlayable) {
            // Those Playbacks are the same -> Nothing to do.
            samePlayable
          } else {
            // Scenario: rebind a bound Playable from one Container to other Container that is being
            // bound.
            // Action: remove both 'sameContainer' and 'samePlayable', create new one for the
            // Container
            // FIXME(eneim): why not calling these after `playable.playback` setter?
            sameContainer.manager.removePlayback(sameContainer)
            samePlayable.manager.removePlayback(samePlayable, clearPlayable = false)
            val newPlayback = createNewPlayback()
            newPlayback.bindPlayable(playable, payload.state)
            newPlayback
          }
        }
      }

    manager.refresh() // Kick it.
    "Request[${hexCode()}]_BIND_End result=$boundPlayback".logDebug()
    return boundPlayback
  }

  override fun toString(): String = "R@${hexCode()}"

  private fun Playback.bindPlayable(
    playable: Playable,
    state: PlayableState
  ) {
    playable.playback = this
    playable.onBind(playback = this, state = state)
    manager.addPlayback(this)
  }
}
