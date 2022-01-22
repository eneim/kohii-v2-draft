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

import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A class that acts as a [AdComponentsListener] and a [MutableSet] of [AdComponentsListener] at
 * the same time.
 */
class AdComponentsListeners(
  private val listeners: MutableSet<AdComponentsListener>
) : MutableSet<AdComponentsListener> by listeners, AdComponentsListener {

  constructor() : this(CopyOnWriteArraySet<AdComponentsListener>())

  override fun onAdEvent(event: AdEvent) = forEach { it.onAdEvent(event) }

  override fun onAdError(error: AdErrorEvent?) = forEach { it.onAdError(error) }

  override fun onAdProgress(
    mediaInfo: AdMediaInfo,
    progressUpdate: VideoProgressUpdate
  ) = forEach { it.onAdProgress(mediaInfo, progressUpdate) }

  override fun onBuffering(mediaInfo: AdMediaInfo) = forEach { it.onBuffering(mediaInfo) }

  override fun onContentComplete() = forEach { it.onContentComplete() }

  override fun onEnded(mediaInfo: AdMediaInfo) = forEach { it.onEnded(mediaInfo) }

  override fun onError(mediaInfo: AdMediaInfo) = forEach { it.onError(mediaInfo) }

  override fun onLoaded(mediaInfo: AdMediaInfo) = forEach { it.onLoaded(mediaInfo) }

  override fun onPause(mediaInfo: AdMediaInfo) = forEach { it.onPause(mediaInfo) }

  override fun onPlay(mediaInfo: AdMediaInfo) = forEach { it.onPlay(mediaInfo) }

  override fun onResume(mediaInfo: AdMediaInfo) = forEach { it.onResume(mediaInfo) }

  override fun onVolumeChanged(
    mediaInfo: AdMediaInfo,
    percentage: Int
  ) = forEach { it.onVolumeChanged(mediaInfo, percentage) }
}
