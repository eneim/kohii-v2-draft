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
import kohii.v2.core.Playable.Controller
import kohii.v2.exoplayer.ComponentsListener
import kohii.v2.exoplayer.ComponentsListeners

abstract class AbstractBridge<RENDERER : Any> : Bridge<RENDERER> {

  override var controller: Controller = Controller

  protected val componentsListeners: ComponentsListeners = ComponentsListeners()

  final override fun addComponentsListener(listener: ComponentsListener) {
    componentsListeners.add(listener)
  }

  final override fun removeComponentsListener(listener: ComponentsListener?) {
    componentsListeners.remove(listener)
  }

  @CallSuper
  override fun release() {
    componentsListeners.clear()
  }
}
