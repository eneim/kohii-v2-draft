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

package kohii.v2.demo.screens.multiurls

import androidx.media3.common.MediaItem
import kohii.v2.core.RequestData
import kotlinx.parcelize.Parcelize

abstract class MultiUrlsData(
  open val previewUrl: String,
  open val mainUrl: String,
) : RequestData {

  override fun isCompatible(other: RequestData): Boolean {
    return other is MultiUrlsData && other.previewUrl == previewUrl && other.mainUrl == mainUrl
  }
}

@Parcelize
data class PreviewVideoData(
  override val previewUrl: String,
  override val mainUrl: String,
) : MultiUrlsData(previewUrl, mainUrl) {

  override fun toMediaItem(): MediaItem = MediaItem.fromUri(previewUrl)
}

@Parcelize
data class MainVideoData(
  override val previewUrl: String,
  override val mainUrl: String,
) : MultiUrlsData(previewUrl, mainUrl) {

  override fun toMediaItem(): MediaItem = MediaItem.fromUri(mainUrl)
}
