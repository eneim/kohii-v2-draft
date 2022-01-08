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
import kohii.v2.internal.logInfo
import kohii.v2.internal.logWarn
import kohii.v2.core.PlayableState.Initialized
import kohii.v2.internal.asString
import kohii.v2.internal.hexCode

/**
 * @param firstManager The first [PlayableManager] that manages this [Playable]. In practice, a
 * [Playable] can be managed by different [PlayableManager]s in different times.
 */
abstract class AbstractPlayable(
  home: Home,
  tag: String,
  data: Any,
  rendererType: Class<*>,
  firstManager: PlayableManager,
) : Playable(
  home = home,
  tag = tag,
  data = data,
  rendererType = rendererType,
  initialManager = firstManager
) {

  /**
   * Returns the immediate state of this [Playable], represented by a [PlayableState].
   */
  override fun currentState(): PlayableState = Initialized

  @CallSuper
  override fun onRelease() {
    "Playable[${hexCode()}]_RELEASE".logInfo()
  }

  @CallSuper
  override fun onRendererAttached(renderer: Any?) {
    "Playable[${hexCode()}]_ATTACH_Renderer [RR=${renderer?.asString()}]".logWarn()
  }

  @CallSuper
  override fun onRendererDetached(renderer: Any?) {
    "Playable[${hexCode()}]_DETACH_Renderer [RR=${renderer?.asString()}]".logWarn()
  }
}
