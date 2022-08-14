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

import kohii.v2.core.LifecycleCallback
import kohii.v2.core.Playable
import kohii.v2.core.Playback
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Used by [Playable] to register a [LifecycleCallback] to the [Playback] it is bound to.
 */
internal class LifecycleCallbackDelegate(val playable: Playable) : LifecycleCallback {

  override fun onAdded(playback: Playback) = Unit

  override fun onRemoved(playback: Playback) = Unit

  override fun onAttached(playback: Playback) = Unit

  override fun onDetached(playback: Playback) = Unit

  override fun onActivated(playback: Playback) {
    playable.tryRestorePlayableState()
  }

  override fun onDeactivated(playback: Playback) {
    if (
    // The provided Playback must be the same one bound to the current Playable.
      playback === playable.playback &&
      // If the Playback is being removed, either naturally (1), or to be replaced by another one
      // that uses the same Playable (2), we don't save the Playable state:
      // In (1), there is no point to save the state.
      // In (2), we want the Playable to keep playing as-if it is still bound to a Playback.
      !playback.isRemoving &&
      // If this is a destruction for recreation, we want to keep the Playable alive and unchanged.
      // If after the recreation, the Playable is not bound to a new Playback, it will be destroyed.
      !playback.manager.isChangingConfigurations
    ) {
      playable.trySavePlayableState()
      playable.onRelease()
    }
  }
}

internal fun Playable.playbackLifecycleCallback(): Lazy<LifecycleCallback> =
  lazy(NONE) { LifecycleCallbackDelegate(playable = this) }
