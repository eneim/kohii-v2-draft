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

package kohii.v2.demo

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.airbnb.epoxy.SimpleEpoxyModel
import com.google.android.exoplayer2.MediaItem
import kohii.v2.core.Engine
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.RequestHandle
import kohii.v2.demo.common.BaseDemoFragment
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentSimpleRecyclerViewBinding
import kohii.v2.demo.databinding.HolderMultipleVideosContainerBinding

/**
 * A RecyclerView Fragment with 2 Videos in the same ViewHolder.
 */
class MixedVideosInRecyclerViewFragment : BaseDemoFragment(R.layout.fragment_simple_recycler_view) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentSimpleRecyclerViewBinding = FragmentSimpleRecyclerViewBinding.bind(view)
    val engine: Engine = ExoPlayerEngine(binding.videos)

    binding.videos.withModels {
      object : SimpleEpoxyModel(R.layout.holder_multiple_videos_container) {

        private val requests = mutableListOf<RequestHandle>()

        override fun bind(view: View) {
          super.bind(view)
          val holder = HolderMultipleVideosContainerBinding.bind(view)
          requests.onEach(RequestHandle::cancel).clear()

          requests += engine.setUp(
            data = MediaItem.Builder().setUri(VideoUrls.LocalHevc).build(),
            tag = "$initSeed::${VideoUrls.LocalHevc}::FIRST"
          )
            .bind(container = holder.firstVideo)

          requests += engine.setUp(
            data = MediaItem.Builder().setUri(VideoUrls.LocalHevc).build(),
            tag = "$initSeed::${VideoUrls.LocalHevc}::SECOND"
          )
            .bind(container = holder.secondVideo)
        }

        override fun unbind(view: View) {
          super.unbind(view)
          requests.onEach(RequestHandle::cancel).clear()
        }
      }
        .id(VideoUrls.LocalHevc)
        .addTo(this)

      (0 until 8).forEach { index ->
        SimpleEpoxyModel(R.layout.holder_text)
          .id(index + 100)
          .addTo(this)
      }
    }
  }

  companion object {

    fun newInstance(initSeed: String): MixedVideosInRecyclerViewFragment =
      MixedVideosInRecyclerViewFragment().apply {
        arguments = bundleOf(ARGS_INIT_SEED to initSeed)
      }
  }
}
