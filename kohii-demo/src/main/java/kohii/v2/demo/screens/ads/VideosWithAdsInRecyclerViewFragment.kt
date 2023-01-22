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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckedTextView
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.SimpleEpoxyModel
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.RequestHandle
import kohii.v2.demo.R
import kohii.v2.demo.databinding.FragmentVideosWithAdsBinding
import kohii.v2.demo.demoApp
import kohii.v2.demo.home.DemoItemFragment
import kohii.v2.exoplayer.DefaultVideoAdPlayerCallback
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okio.buffer
import okio.source

@UnstableApi
class VideosWithAdsInRecyclerViewFragment : DemoItemFragment(R.layout.fragment_videos_with_ads) {

  private val viewModel: VideoWithAdsViewModel by viewModels()

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideosWithAdsBinding.bind(view)
    val engine = ExoPlayerEngine()
    engine.useBucket(binding.videoContainer)

    val adsSamples = demoApp.moshi
      .adapter(AdSamples::class.java)
      .fromJson(demoApp.assets.open("media/media_with_ads.json").source().buffer())
      ?: AdSamples("No Ads", emptyList())

    var handle: RequestHandle? = null

    viewModel.selectedItem
      .onEach { (_, selectedAd) ->
        handle?.cancel()

        if (selectedAd != null) {
          binding.selectedAdTitle.text = selectedAd.name
          handle = engine.setUp(data = selectedAd.toRequestData())
            .withTag("$seed::${selectedAd.name}")
            .bind(binding.videoContainer) {
              addAdEventListener { Log.i("Kohii~Ad", "AdEvent: $it") }
              addAdErrorListener { Log.w("Kohii~Ad", "AdError: $it") }
              addVideoAdPlayerCallback(object : DefaultVideoAdPlayerCallback {
                override fun onAdProgress(
                  mediaInfo: AdMediaInfo,
                  progressUpdate: VideoProgressUpdate,
                ) {
                  binding.selectedAdTitle.text =
                    "${selectedAd.name} | ${progressUpdate.currentTimeMs}ms"
                }
              })
            }
        } else {
          binding.selectedAdTitle.text = "No ad selected."
        }
      }
      .launchIn(viewLifecycleOwner.lifecycleScope)

    binding.videos.addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))

    val itemMinHeight = resources.getDimensionPixelSize(R.dimen.text_item_min_height)

    // This part uses the Epoxy library for quick setup.
    binding.videos.withModels {
      adsSamples.samples.forEachIndexed { _, adSample: AdSample ->
        object : SimpleEpoxyModel(android.R.layout.simple_list_item_single_choice) {
          override fun bind(view: View) {
            super.bind(view)
            view.minimumHeight = itemMinHeight
            view.updateLayoutParams { height = WRAP_CONTENT }

            val isSelected = (viewModel.selectedItem.value.second == adSample)
            (view as CheckedTextView).apply {
              isChecked = isSelected
              text = adSample.name
            }
          }

          override fun unbind(view: View) {
            super.unbind(view)
            (view as CheckedTextView).isChecked = false
          }
        }
          .onClick {
            val currentSelection = viewModel.selectedItem.value
            if (currentSelection.second != adSample) {
              viewModel.setSelection(adSample)
            } else {
              viewModel.setSelection(null)
            }
            requestModelBuild()
          }
          .id(adSample.hashCode())
          .addTo(this)
      }
    }
  }
}

