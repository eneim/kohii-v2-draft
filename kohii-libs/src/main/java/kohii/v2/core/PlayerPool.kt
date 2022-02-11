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

import android.os.Build
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.collection.arraySetOf
import kotlin.math.max

/**
 * Definition of a pool to provide [PLAYER] instance for the consumer.
 */
// TODO: put an optional expiration so that it will cleanup unused instance after some time.
abstract class PlayerPool<PLAYER : Any> @JvmOverloads constructor(
  @IntRange(from = 1) val poolSize: Int = DEFAULT_POOL_SIZE,
) {

  init {
    require(poolSize > 0) { "Pool size must be positive." }
  }

  private val playerPool = arraySetOf<PLAYER>()

  /**
   * Returns `true` if the [PLAYER] instance can be used to play the [mediaData], `false` otherwise.
   */
  protected abstract fun PLAYER.accept(mediaData: Any): Boolean

  /**
   * Resets the internal state of the [player] instance before putting it back to the pool. The
   * player instance still be usable with a preparation.
   *
   * @param player The [PLAYER] instance to reset.
   */
  protected abstract fun resetPlayer(player: PLAYER)

  /**
   * Creates a new [PLAYER] instance that can be used to play the [mediaData] object.
   *
   * @param mediaData The media data.
   * @return a [PLAYER] instance that can be used to play the [mediaData].
   */
  protected abstract fun createPlayer(mediaData: Any): PLAYER

  /**
   * Destroys the [PLAYER] instance. After this, the [player] must not be reusable.
   *
   * @param player The [PLAYER] instance.
   */
  protected abstract fun destroyPlayer(player: PLAYER)

  /**
   * Acquires a [PLAYER] instance that can be used to play the [mediaData] from the pool. If there
   * is no available instance in the pool, the client will be asked to create a new one.
   *
   * @param mediaData The media data.
   * @return a [PLAYER] instance that can be used to play the [mediaData].
   */
  fun getPlayer(mediaData: Any): PLAYER = playerPool.find { it.accept(mediaData) }
    ?.also { playerPool.remove(it) }
    ?: createPlayer(mediaData)

  /**
   * Releases an unused [PLAYER] to the pool. If the pool is already full, the client needs to
   * destroy the [PLAYER] instance. Return `true` if the instance is successfully put back to the
   * pool, or `false` otherwise.
   *
   * @param player The [PLAYER] to be put back to the pool.
   * @return `true` if the instance is successfully put back to the pool, or `false` otherwise.
   */
  fun putPlayer(player: PLAYER): Boolean {
    return if (!playerPool.release(player, poolSize)) {
      destroyPlayer(player)
      false
    } else {
      resetPlayer(player)
      true
    }
  }

  /**
   * Destroy all available [PLAYER] instances in the pool.
   */
  @CallSuper
  open fun clear() {
    playerPool.onEach(::destroyPlayer).clear()
  }

  companion object {
    // Max number of Player instances that are cached in the Pool
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 21 ~ 23 will set pool size to 3, etc.
    val DEFAULT_POOL_SIZE =
      max(Build.VERSION.SDK_INT / 6, max(Runtime.getRuntime().availableProcessors(), 1))

    private fun <T> MutableSet<T>.release(
      item: T,
      maxSize: Int,
    ): Boolean {
      check(!contains(item)) { "Already in the pool!" }
      return if (size < maxSize) {
        add(item)
        true
      } else {
        false
      }
    }
  }
}
