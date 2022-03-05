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

import androidx.annotation.CallSuper
import androidx.annotation.MainThread

/**
 * A class that can create [Playable] for a specific piece of data.
 *
 * Instance of this class should be able to reused for multiple [Engine]s that support the same
 * renderer type.
 */
abstract class PlayableCreator {

  /**
   * Creates a new [Playable] with the [data], [tag] and initially managed by the [playableManager].
   */
  abstract fun createPlayable(
    playableManager: PlayableManager,
    data: List<RequestData>,
    tag: String,
  ): Playable

  private var attachCount: Int = 0

  /**
   * Called when the [PlayableCreator] is not attached to any [Engine]. Implementation should clear
   * any resource used by this class.
   */
  @MainThread
  protected open fun onClear() = Unit

  /**
   * Called when the lifecycle attached to the [Engine] is created.
   */
  @CallSuper
  @MainThread
  open fun onAttached() {
    attachCount++
  }

  /**
   * Called when the lifecycle attached to the [Engine] is destroyed.
   */
  @CallSuper
  @MainThread
  open fun onDetached() {
    attachCount--
    if (attachCount <= 0) onClear()
  }
}
