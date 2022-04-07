/*
 * Copyright (c) 2022. Nam Nguyen, nam@ene.im
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

import android.os.Parcel
import android.os.Parcelable
import androidx.media3.common.MediaItem

/**
 * A class that needs to be a [Parcelable] and knows how to build a [MediaItem]. Client can
 * implement this class to provide a custom behavior, such as to retain the full data of a
 * [MediaItem] after configuration change. Note that, the default behavior of a [MediaItem] ignores
 * the [MediaItem.localConfiguration] across serializations.
 */
interface RequestData : Parcelable {

  /**
   * An idempotent method that builds a [MediaItem]. That is, even if this class is serialized to
   * [Parcel] and deserialized back, this method must returns the equaled instance.
   */
  fun toMediaItem(): MediaItem

  /**
   * Returns `true` if this instance is compatible with `other`. This method is default to
   * [equals]. Compatible [RequestData]s can share the same [PlayableState]. Client can provide
   * custom behavior to tell about the compatibility of two [RequestData]s.
   */
  fun isCompatible(other: RequestData): Boolean = this == other
}

@JvmSynthetic
internal fun List<RequestData>.isCompatible(other: List<RequestData>): Boolean {
  // Returns `false` if one list doesn't have a compatible item with an item on the another.
  return (this.size == other.size) &&
    this.all(other::hasCompatibleItem) &&
    other.all(this::hasCompatibleItem)
}

private fun List<RequestData>.hasCompatibleItem(item: RequestData): Boolean =
  any { it.isCompatible(item) }
