/*
 * Copyright (c) 2022. Nam Nguyen, nam@ene.im
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

import kohii.v2.core.Playable
import kohii.v2.core.Playback

internal sealed class BindingType(val playable: Playable) {

  abstract fun performBind(createNewPlayback: () -> Playback): Playback

  // Bind a new Playable to a clean Container.
  class BindToCleanContainer(playable: Playable) : BindingType(playable) {

    override fun performBind(createNewPlayback: () -> Playback): Playback {
      val newPlayback = createNewPlayback()
      newPlayback.bindPlayable(playable)
      return newPlayback
    }
  }

  // Bind a new Playable to a bound Container.
  // Action: create new Playback for the Container, establish the new binding and remove old
  // binding of the Container.
  class BindToBoundContainer(
    playable: Playable,
    val containerCurrentPlayback: Playback,
  ) : BindingType(playable) {

    override fun performBind(createNewPlayback: () -> Playback): Playback {
      containerCurrentPlayback.manager.removePlayback(containerCurrentPlayback)
      val newPlayback = createNewPlayback()
      newPlayback.bindPlayable(playable)
      return newPlayback
    }
  }

  // (Re)bind a bound Playable to a clean Container.
  // Action: create new Playback for the Container, establish the new binding and remove
  // the old Playback of the Playable.
  class RebindToCleanContainer(
    playable: Playable,
    val playableCurrentPlayback: Playback,
  ) : BindingType(playable) {

    override fun performBind(createNewPlayback: () -> Playback): Playback {
      playableCurrentPlayback.manager.removePlayback(playableCurrentPlayback, clearPlayable = false)
      val newPlayback = createNewPlayback()
      newPlayback.bindPlayable(playable)
      return newPlayback
    }
  }

  // (Re)bind a bound Playable to a bound Container
  // Action: if the current Playbacks are different -> remove both and create new one for the new
  // binding.
  class RebindToBoundContainer(
    playable: Playable,
    val playableCurrentPlayback: Playback,
    val containerCurrentPlayback: Playback,
  ) : BindingType(playable) {
    override fun performBind(createNewPlayback: () -> Playback): Playback {
      return if (playableCurrentPlayback === containerCurrentPlayback) {
        playableCurrentPlayback
      } else {
        containerCurrentPlayback.manager.removePlayback(containerCurrentPlayback)
        playableCurrentPlayback.manager.removePlayback(
          playableCurrentPlayback, clearPlayable = false
        )
        val newPlayback = createNewPlayback()
        newPlayback.bindPlayable(playable)
        newPlayback
      }
    }
  }

  companion object {

    fun get(
      playable: Playable,
      samePlayable: Playback?,
      sameContainer: Playback?,
    ): BindingType = if (samePlayable != null) {
      if (sameContainer != null) {
        RebindToBoundContainer(
          playable = playable,
          playableCurrentPlayback = samePlayable,
          containerCurrentPlayback = sameContainer,
        )
      } else {
        RebindToCleanContainer(
          playable = playable,
          playableCurrentPlayback = samePlayable,
        )
      }
    } else {
      if (sameContainer != null) {
        BindToBoundContainer(
          playable = playable,
          containerCurrentPlayback = sameContainer,
        )
      } else {
        BindToCleanContainer(playable = playable)
      }
    }
  }
}

private fun Playback.bindPlayable(playable: Playable) {
  playable.playback = this
  manager.addPlayback(this)
}

