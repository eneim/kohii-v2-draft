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

package kohii.v2.demo.screens.mixed

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import com.airbnb.epoxy.SimpleEpoxyModel
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.RequestHandle
import kohii.v2.demo.R.layout
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentSimpleRecyclerViewBinding
import kohii.v2.demo.databinding.HolderMultipleVideosContainerBinding
import kohii.v2.demo.home.DemoItemFragment

/**
 * A RecyclerView Fragment with 2 Videos in the same ViewHolder.
 */
class MixedVideosInRecyclerViewFragment : DemoItemFragment(layout.fragment_simple_recycler_view) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentSimpleRecyclerViewBinding = FragmentSimpleRecyclerViewBinding.bind(view)
    val engine = ExoPlayerEngine()
    engine.useBucket(binding.videos)

    binding.videos.withModels {
      object : SimpleEpoxyModel(layout.holder_multiple_videos_container) {

        private val requests = mutableListOf<RequestHandle>()

        override fun bind(view: View) {
          super.bind(view)
          val holder = HolderMultipleVideosContainerBinding.bind(view)
          requests.onEach(RequestHandle::cancel).clear()

          requests += engine.setUp(
            tag = "$seed::${VideoUrls.SINTEL_MPD}::FIRST",
            data = VideoUrls.SINTEL_MPD,
          )
            .withCallback { playback, request ->
              Log.i("Mixed", "Playback: $playback, Request: $request")
            }
            .bind(container = holder.firstVideo)

          /* requests += engine.setUp(
            tag = "$seed::${VideoUrls.LOCAL_BBB_HEVC}::SECOND",
            data = VideoUrls.LOCAL_BBB_HEVC,
          )
            .bind(container = holder.secondVideo) */
        }

        override fun unbind(view: View) {
          super.unbind(view)
          requests.onEach(RequestHandle::cancel).clear()
        }
      }
        .id("${VideoUrls.SINTEL_MPD}, ${VideoUrls.LOCAL_BBB_HEVC}")
        .addTo(this)

      (0 until 8).forEach { index ->
        SimpleEpoxyModel(layout.holder_text)
          .id(index + 100)
          .addTo(this)
      }
    }
  }

  companion object {

    fun newInstance(seed: String): MixedVideosInRecyclerViewFragment =
      MixedVideosInRecyclerViewFragment().apply {
        arguments = bundleOf(KEY_SEED to seed)
      }
  }
}
