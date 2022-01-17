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

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableManager

internal class ManagerViewModel(
  application: Application,
  private val stateHandle: SavedStateHandle
) : AndroidViewModel(application), PlayableManager {

  private val home: Home = Home[application]

  /**
   * A [Set] of [Playable]s managed by this class.
   */
  private val playables = mutableSetOf<Playable>()

  override fun toString(): String = "PM@${hexCode()}"

  override fun addPlayable(
    playable: Playable,
    state: Bundle?,
  ) {
    "PlayableManager[${hexCode()}]_ADD_Playable [PB=$playable]".logInfo()
    if (playables.add(playable)) {
      "PlayableManager[${hexCode()}]_ADDED_Playable [PB=$playable]".logDebug()
      if (state != null) {
        stateHandle[playable.stateKey] = state
      }
    }
  }

  override fun removePlayable(playable: Playable): Bundle? {
    "PlayableManager[${hexCode()}]_REMOVE_Playable [PB=$playable]".logInfo()
    return if (playable.manager === this && playables.remove(playable)) {
      "PlayableManager[${hexCode()}]_REMOVED_Playable [PB=$playable]".logDebug()
      stateHandle.remove<Bundle>(playable.stateKey)
    } else {
      null
    }
  }

  override fun fetchPlayableState(playable: Playable): Bundle? {
    return stateHandle.get(playable.stateKey)
  }

  override fun trySavePlayableState(playable: Playable) {
    "PlayableManager[${hexCode()}]_SAVE_Playable [PB=$playable] [PK=${playable.playback}]".logInfo()
    val playableState = playable.onSaveState()
    if (playableState != Bundle.EMPTY) {
      stateHandle[playable.stateKey] = playableState
    }
  }

  override fun tryRestorePlayableState(playable: Playable) {
    "PlayableManager[${hexCode()}]_RESTORE_Playable [PB=$playable] [PK=${playable.playback}]".logInfo()
    val savedState: Bundle? = stateHandle.remove<Bundle>(playable.stateKey)
    savedState?.let(playable::onRestoreState)
    "PlayableManager[${hexCode()}]_RESTORED_Playable [PB=$playable] [state=$savedState]".logDebug()
  }

  override fun onCleared() {
    super.onCleared()
    for (playable in playables) {
      debugOnly { check(playable.manager === this) }
      home.destroyPlayable(playable)
    }
  }
}

private val Playable.stateKey: String get() = tag.takeIf { it.isNotEmpty() } ?: internalId
