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

package kohii.v2.demo.screens.ads

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.AdsConfiguration
import kohii.v2.core.RequestData
import kotlinx.parcelize.Parcelize

/**
 * A [RequestData] to support a [AdSample].
 */
@JvmInline
@Parcelize
internal value class AdMediaData(val media: AdSample) : RequestData {

  override fun toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(media.contentUri)
    .setTag(media.name)
    .setAdsConfiguration(AdsConfiguration.Builder(media.adTagUri).build())
    .build()

  override fun isSame(other: RequestData): Boolean {
    return other is AdMediaData && media == other.media
  }
}
