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

import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.exoplayer2.MediaItem.AdsConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.ui.AdOverlayInfo
import com.google.android.exoplayer2.ui.AdViewProvider

/**
 * A helper class used by the [StyledPlayerViewBridge] to setup ads playback.
 *
 * This class uses a prebuilt [AdsLoader] but doesn't use the provided [AdsConfiguration] to
 * create the [AdsLoader] instance.
 */
internal class ImaSetupBundle(
  val adsLoader: ImaAdsLoader,
  val adViewGroup: FrameLayout,
) : AdsLoader.Provider, AdViewProvider {

  override fun getAdsLoader(adsConfiguration: AdsConfiguration): AdsLoader = adsLoader
  override fun getAdViewGroup(): ViewGroup = adViewGroup
  override fun getAdOverlayInfos(): MutableList<AdOverlayInfo> = mutableListOf()

  // Must be called before player.prepare()
  fun ready(player: Player) {
    adsLoader.setPlayer(player)
  }

  fun reset() {
    adsLoader.setPlayer(null)
  }

  fun release() {
    adsLoader.setPlayer(null)
    adsLoader.release()
  }
}
