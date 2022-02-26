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

package kohii.v2.demo.ads

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckedTextView
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.SimpleEpoxyModel
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.RequestHandle
import kohii.v2.demo.R
import kohii.v2.demo.common.flowWithPrevious
import kohii.v2.demo.databinding.FragmentVideosWithAdsBinding
import kohii.v2.demo.demoApp
import kohii.v2.exoplayer.DefaultVideoAdPlayerCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import okio.buffer
import okio.source
import kotlin.LazyThreadSafetyMode.NONE

class VideosWithAdsInRecyclerViewFragment : Fragment(R.layout.fragment_videos_with_ads) {

  private val viewModel: VideoWithAdsViewModel by viewModels()
  private val seed: String by lazy(NONE) { requireArguments().getString(KEY_SEED).orEmpty() }

  @OptIn(ExperimentalKohiiApi::class)
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideosWithAdsBinding.bind(view)
    val engine = ExoPlayerEngine(binding.videoContainer)

    val adsSamples = demoApp.moshi
      .adapter(AdSamples::class.java)
      .fromJson(demoApp.assets.open("media/media_with_ads.json").source().buffer())
      ?: AdSamples("No Ads", emptyList())

    var handle: RequestHandle?

    viewModel.selectedAd
      .onEach { (prevAd, selectedAd) ->
        engine.home.cancel(prevAd?.name.orEmpty())

        if (selectedAd != null) {
          binding.selectedAdTitle.text = selectedAd.name
          handle = engine.setUp(
            data = selectedAd.toMediaItem(),
            tag = "$seed::${selectedAd.name}"
          )
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

    binding.videos.withModels {
      adsSamples.samples.forEachIndexed { _, adSample: AdSample ->
        object : SimpleEpoxyModel(android.R.layout.simple_list_item_single_choice) {
          override fun bind(view: View) {
            super.bind(view)
            view.minimumHeight = itemMinHeight
            view.updateLayoutParams { height = WRAP_CONTENT }

            val itemSelected = (viewModel.selectedAd.value.second == adSample)
            (view as CheckedTextView).apply {
              isChecked = itemSelected
              text = adSample.name
            }
          }

          override fun unbind(view: View) {
            super.unbind(view)
            (view as CheckedTextView).isChecked = false
          }
        }
          .onClick {
            val currentSelection = viewModel.selectedAd.value
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

  companion object {
    private const val KEY_SEED = "KEY_SEED"

    fun getInstance(position: Int): VideosWithAdsInRecyclerViewFragment =
      VideosWithAdsInRecyclerViewFragment().apply {
        arguments = bundleOf(KEY_SEED to "seed::$position")
      }
  }
}

internal class VideoWithAdsViewModel : ViewModel() {

  private val _selectedAd = MutableStateFlow<AdSample?>(null)

  val selectedAd: StateFlow<Pair<AdSample?, AdSample?>> = _selectedAd
    .flowWithPrevious()
    .drop(1)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = Pair(null, null)
    )

  fun setSelection(adSample: AdSample?) {
    _selectedAd.value = adSample
  }
}
