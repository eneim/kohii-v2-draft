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

package kohii.v2.core

import android.os.Bundle

interface PlayableManager {

  /**
   * Adds [playable] to this manager, with an optional initial state.
   */
  fun addPlayable(
    playable: Playable,
    state: Bundle? = null,
  )

  /**
   * Removes [playable] from this manager, returns the last saved state as a [Bundle].
   *
   * This method is called by the [Playable] to remove itself from the current [PlayableManager]
   * before adding to a new [PlayableManager], or before the destruction.
   */
  fun removePlayable(playable: Playable): Bundle?

  /**
   * Returns the currently saved state of the [Playable] in this manager.
   */
  fun getPlayableState(playable: Playable): Bundle?

  /**
   * Saves the current state of the [Playable].
   *
   * The state of a [Playable] should be saved for restoration when the bound [Playback] is
   * deactivated temporarily (for example, the user scrolls and the PlayerView is hidden, but not
   * removed from the RecyclerView). There is no need to save and restore the state if the
   * [Playback] is being removed (in which case, the Playback is also deactivated before removing).
   *
   * @param playable The [Playable] whose state should be saved.
   */
  fun trySavePlayableState(playable: Playable)

  /**
   * Restores the [Playable] using a state saved previously by [trySavePlayableState].
   */
  fun tryRestorePlayableState(playable: Playable)
}
