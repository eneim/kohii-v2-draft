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

package kohii.v2.exoplayer

import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer.VideoAdPlayerCallback
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate

/**
 * A default [VideoAdPlayerCallback] that does nothing.
 */
interface DefaultVideoAdPlayerCallback : VideoAdPlayerCallback {

  override fun onAdProgress(
    mediaInfo: AdMediaInfo,
    progressUpdate: VideoProgressUpdate,
  ) = Unit

  override fun onBuffering(mediaInfo: AdMediaInfo) = Unit

  override fun onContentComplete() = Unit

  override fun onEnded(mediaInfo: AdMediaInfo) = Unit

  override fun onError(mediaInfo: AdMediaInfo) = Unit

  override fun onLoaded(mediaInfo: AdMediaInfo) = Unit

  override fun onPause(mediaInfo: AdMediaInfo) = Unit

  override fun onPlay(mediaInfo: AdMediaInfo) = Unit

  override fun onResume(mediaInfo: AdMediaInfo) = Unit

  override fun onVolumeChanged(
    mediaInfo: AdMediaInfo,
    percentage: Int,
  ) = Unit
}
