package kohii.v2.core

import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.core.util.Pools.Pool
import androidx.core.util.Pools.SimplePool
import kohii.v2.common.debugOnly
import kohii.v2.common.logInfo
import kohii.v2.internal.asString
import kohii.v2.internal.hexCode
import kohii.v2.internal.onEachAcquired

abstract class RecycledRendererProvider @JvmOverloads constructor(
  private val poolSize: Int = 2
) : StatefulRendererProvider() {

  private val pools = SparseArrayCompat<Pool<Any>>(2)

  override fun provideRenderer(playable: Playable, playback: Playback): Any? {
    val rendererType: Int = getRendererType(playback.container, playable.data)
    val pool = pools.get(rendererType)
    val result = pool?.acquire() ?: createRenderer(playback, rendererType)
    "RendererProvider[${hexCode()}]_PROVIDE [RR=${result?.asString()}]".logInfo()
    return result
  }

  override fun releaseRenderer(playable: Playable, playback: Playback, renderer: Any?) {
    "RendererProvider[${hexCode()}]_RELEASE [RR=${renderer?.asString()}]".logInfo()
    if (renderer == null) return
    val rendererType: Int = getRendererType(playback.container, playable.data)
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

  protected abstract fun createRenderer(
    playback: Playback,
    rendererType: Int
  ): Any?

  override fun onClear() {
    pools.forEach { _, value -> value.onEachAcquired { /* Do nothing */ } }
    pools.clear()
  }

  override fun toString(): String {
    return "RP@${hexCode()}"
  }
}
