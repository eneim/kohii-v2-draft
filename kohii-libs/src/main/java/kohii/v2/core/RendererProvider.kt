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

/**
 * A lifecycle-aware class that provides renderer for the [Playback] when needed.
 */
abstract class RendererProvider : DefaultLifecycleObserver {

  /**
   * Returns `true` if this [RendererProvider] accepts [playback] and can provide it a renderer.
   */
  abstract fun accept(playback: Playback): Boolean

  /**
   * Provides a renderer to [playback].
   */
  abstract fun provideRenderer(
    playback: Playback,
  ): Any?

  /**
   * Puts the [renderer] that was used by [playback] to the cache, or clear it if this
   * [RendererProvider] doesn't cache.
   */
  abstract fun releaseRenderer(
    playback: Playback,
    renderer: Any?,
  )
}
