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

import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.core.util.Pools.Pool
import androidx.core.util.Pools.SimplePool
import androidx.lifecycle.LifecycleOwner
import kohii.v2.internal.debugOnly
import kohii.v2.internal.logInfo
import kohii.v2.internal.asString
import kohii.v2.internal.hexCode
import kohii.v2.internal.onEachAcquired

abstract class RecycledRendererProvider @JvmOverloads constructor(
  private val poolSize: Int = 2
) : RendererProvider() {

  private val pools = SparseArrayCompat<Pool<Any>>(2)

  final override fun provideRenderer(playback: Playback): Any? {
    val rendererType: Int = getRendererType(playback.container, playback.playable.data)
    val pool = pools.get(rendererType)
    val result = pool?.acquire() ?: createRenderer(playback, rendererType)
    "RendererProvider[${hexCode()}]_PROVIDE [RR=${result?.asString()}]".logInfo()
    return result
  }

  final override fun releaseRenderer(playback: Playback, renderer: Any?) {
    "RendererProvider[${hexCode()}]_RELEASE [RR=${renderer?.asString()}]".logInfo()
    if (renderer == null) return
    recycleRenderer(renderer)
    val rendererType: Int = getRendererType(playback.container, playback.playable.data)
    val pool = pools.get(rendererType) ?: SimplePool<Any>(poolSize).also {
      pools.put(rendererType, it)
    }
    try {
      pool.release(renderer)
    } catch (error: IllegalStateException) {
      debugOnly(error::printStackTrace)
    }
  }

  protected open fun getRendererType(
    container: Any,
    data: Any,
  ): Int = 0

  /**
   * Creates a new renderer instance if possible.
   */
  protected abstract fun createRenderer(
    playback: Playback,
    rendererType: Int
  ): Any?

  /**
   * Sanitizes the [renderer] before releasing it back to the Pool.
   */
  protected abstract fun recycleRenderer(renderer: Any)

  /**
   * Clears any resource used by the [renderer] before removing it from memory.
   */
  protected open fun destroyRenderer(renderer: Any?) = Unit

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    pools.forEach { _, pool -> pool.onEachAcquired(::destroyRenderer) }
    pools.clear()
  }

  override fun toString(): String {
    return "RP@${hexCode()}"
  }
}
