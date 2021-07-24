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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kohii.v2.common.logDebug
import kohii.v2.common.logInfo
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableManager

internal class ManagerViewModel(
  application: Application
) : AndroidViewModel(application), PlayableManager {

  private val home: Home = Home[application]

  /**
   * A [Set] of [Playable]s managed by this class.
   */
  private val playables = mutableSetOf<Playable>()

  override fun toString(): String {
    return "PM@${hexCode()}"
  }

  override fun addPlayable(playable: Playable) {
    "PlayableManager_ADD_Playable [PB=$playable]".logInfo()
    if (playables.add(playable)) {
      "PlayableManager_ADDED_Playable [PB=$playable]".logDebug()
      home.keepPlayable(playable)
    }
  }

  override fun removePlayable(playable: Playable) {
    "PlayableManager_REMOVE_Playable [PB=$playable]".logInfo()
    if (playables.remove(playable) && playable.manager === this) {
      "PlayableManager_REMOVED_Playable [PB=$playable]".logDebug()
      home.releasePlayable(playable)
      home.destroyPlayable(playable)
    }
  }

  override fun trySavePlayableState(playable: Playable) {
    "PlayableManager_SAVE_Playable [PB=$playable]".logInfo()
  }

  override fun tryRestorePlayableState(playable: Playable) {
    "PlayableManager_RESTORE_Playable [PB=$playable]".logInfo()
  }

  override fun onCleared() {
    super.onCleared()
    for (playable in playables) {
      home.releasePlayable(playable)
      home.destroyPlayable(playable)
    }
  }
}
