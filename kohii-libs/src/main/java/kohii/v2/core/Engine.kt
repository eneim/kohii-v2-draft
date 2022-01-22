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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * [Engine] instance should have the same lifecycle as the [Manager] instance.
 *
 * If the [RendererProvider] provides [android.view.View]s objects, it is recommended to reuse an
 * instance within the same Activity.
 */
class Engine constructor(
  internal val rendererType: Class<*>,
  internal val manager: Manager,
  internal val playableCreator: PlayableCreator,
  internal val rendererProvider: RendererProvider,
) : DefaultLifecycleObserver {

  val home: Home = manager.home

  init {
    manager.lifecycleOwner.lifecycle.addObserver(this)
  }

  override fun onCreate(owner: LifecycleOwner) {
    manager.addRendererProvider(rendererProvider)
    playableCreator.onAttached()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    owner.lifecycle.removeObserver(this)
    manager.removeRendererProvider(rendererProvider)
    playableCreator.onDetached()
  }

  /**
   * Creates a [Binder] that can be used to bind the media request to a container.
   *
   * If [data] is a [Request] or a [Binder], [tag] will be ignored. Use [Request.copy] or
   * [Binder.copy] to create a new one with a different tag.
   */
  @JvmOverloads
  fun setUp(
    data: Any,
    tag: String? = null
  ): Binder = Binder(
    request = when (data) {
      is Binder -> data.request
      is Request -> data
      else -> Request(data = data, tag = tag)
    },
    engine = this,
  )

  companion object {

    /**
     * Creates a new [Engine] object.
     *
     * @param T Type of the renderer that this [Engine] supports.
     */
    inline fun <reified T> get(
      manager: Manager,
      playableCreator: PlayableCreator,
      rendererProvider: RendererProvider,
    ): Engine = Engine(
      rendererType = T::class.java,
      manager = manager,
      playableCreator = playableCreator,
      rendererProvider = rendererProvider,
    )
  }
}
