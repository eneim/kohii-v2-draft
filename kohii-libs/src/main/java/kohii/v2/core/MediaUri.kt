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

import android.net.Uri
import androidx.media3.common.MediaItem
import kotlinx.parcelize.Parcelize

/**
 * A [RequestData] that is constructed from a [String]. Normally it is the [Uri] of the media
 * content.
 */
@Parcelize
@JvmInline
value class MediaUri(val value: String) : RequestData {

  constructor(uri: Uri) : this(uri.toString())

  override fun toMediaItem(): MediaItem = MediaItem.fromUri(value)

  override fun isCompatible(other: RequestData): Boolean {
    return other is MediaUri && value == other.value
  }
}
