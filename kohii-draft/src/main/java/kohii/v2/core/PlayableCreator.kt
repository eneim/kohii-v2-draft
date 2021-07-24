/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

/**
 * Common definitions for a class that can create [Playable] for a specific piece of data.
 */
interface PlayableCreator {

  /**
   * Returns `true` if this class can create [Playable] instance for the [mediaData].
   */
  fun accepts(mediaData: Any): Boolean

  /**
   * Returns a new [Playable] instance for the [data] and managed by the [playableManager].
   */
  fun createPlayable(playableManager: PlayableManager, data: Any, tag: String): Playable

  /**
   * Cleanup the resource used by this class.
   */
  fun cleanUp()
}
