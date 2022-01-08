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

import kohii.v2.core.Playable
import kohii.v2.core.Playback
import kohii.v2.core.PlaybackEventListener
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Used by [Playable] to register a [PlaybackEventListener] to the [Playback] it is bound to.
 */
internal class PlaybackEventListenerDelegate(val playable: Playable) : PlaybackEventListener {

  override fun onAdded(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_ADDED [PK=$playback]".logInfo()
  }

  override fun onRemoved(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_REMOVED [PK=$playback]".logInfo()
  }

  override fun onAttached(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_ATTACHED [PK=$playback]".logInfo()
  }

  override fun onDetached(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_DETACHED [PK=$playback]".logInfo()
  }

  override fun onActivated(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_ACTIVATED [PK=$playback]".logInfo()
    playable.manager.tryRestorePlayableState(playable)
  }

  override fun onDeactivated(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_DEACTIVATED [PK=$playback]".logInfo()
    // TODO(eneim): there are 4 cases we need to consider:
    //  - 1: The bucket detaches the container, which causes the Playback to be detached, but the
    //  lifecycle of the bucket/manager is unchanged.
    //  - 2: The bucket is detached, which causes the Playback to be detached, though the lifecycle
    //  of the manager is unchanged.
    //  - 3: The manager is destroyed for recreation, which causes the Playback to be detached.
    //  - 4: The manager is destroyed without recreation, which causes the Playback to be detached.
    //  In case 3, we do not want to touch the Playable. In case 1 and 2, we need to save the
    //  Playable state and restore it when the container is attached again. In case 4, we need to
    //  destroy the Playable and release its resources. We may need to think if there is a need to
    //  restore the Playable state after a client re-launch, in which case we will save the Playable
    //  state in case 4 before destroying it. Another scenario we need to save the state is when
    //  the Manager is for a Fragment in a ViewPager.
    if (!playback.manager.isChangingConfigurations) {
      playable.manager.trySavePlayableState(playable)
      // TODO: Allow the client to optionally release the playable on deactivated.
      playable.onRelease()
    }
  }
}

internal fun Playable.playbackEventListener(): Lazy<PlaybackEventListener> =
  lazy(NONE) { PlaybackEventListenerDelegate(playable = this) }
