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
 * If the [rendererProvider] provides [android.view.View]s objects, it is recommended to reuse the
 * same instance for the same Activity.
 */
class Engine constructor(
  internal val rendererType: Class<*>,
  val manager: Manager,
  internal val playableCreator: PlayableCreator,
  internal val rendererProvider: RendererProvider,
) : DefaultLifecycleObserver {

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

  private fun setUpInternal(
    tag: String? = null,
    data: List<RequestData>,
  ): Binder = Binder(
    request = Request(data, tag),
    engine = this,
  )

  /**
   * Creates a [Binder] for an array of [RequestData]s.
   */
  @JvmOverloads
  fun setUp(
    tag: String? = null,
    data: Array<RequestData>,
  ): Binder = setUpInternal(tag, data.toList())

  /**
   * Creates a [Binder] for a single [RequestData].
   */
  @JvmOverloads
  fun setUp(
    tag: String? = null,
    data: RequestData,
  ): Binder = setUpInternal(tag = tag, data = arrayListOf(data))

  /**
   * Creates a [Binder] for a list of [String]s.
   */
  @JvmOverloads
  fun setUp(
    tag: String? = null,
    data: Array<String>,
  ): Binder = setUpInternal(tag = tag, data = data.map(::MediaUri))

  /**
   * Creates a [Binder] for a single [String].
   */
  @JvmOverloads
  fun setUp(
    tag: String? = null,
    data: String,
  ): Binder = setUpInternal(tag = tag, data = arrayListOf(MediaUri(data)))

  /**
   * Creates a new [Binder] using an existing [Request].
   *
   * This method can be used to continue a [Request], for example when the client needs to open a
   * Video in fullscreen, or in a different Window, the original [Request] can be parceled, passed
   * to the new Window and set up there using this method.
   */
  fun setUp(request: Request): Binder = setUpInternal(tag = request.tag, data = request.data)

  /**
   * Registers the [root] as a [Bucket] of the [manager].
   */
  fun useBucket(root: Any): Bucket = manager.bucket(root)

  /**
   * Registers multiple [Bucket] roots.
   */
  fun useBuckets(vararg roots: Any): List<Bucket> = roots.map(manager::bucket)

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
