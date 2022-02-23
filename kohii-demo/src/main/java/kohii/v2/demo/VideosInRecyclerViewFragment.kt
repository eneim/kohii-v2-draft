/*
 * Copyright (c) 2021. Nam Nguyen, nam@ene.im
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
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.SimpleEpoxyModel
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.core.Engine
import kohii.v2.core.Manager
import kohii.v2.core.RequestHandle
import kohii.v2.core.playbackManager
import kohii.v2.demo.VideoInScrollViewFragment.Companion.VIDEO_TAG
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentVideosInRecyclerViewBinding
import kohii.v2.demo.databinding.HolderVideoContainerBinding
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.getStyledPlayerViewProvider
import kotlin.LazyThreadSafetyMode.NONE

class VideosInRecyclerViewFragment : Fragment(R.layout.fragment_videos_in_recycler_view) {

  private val seed: String by lazy(NONE) { requireArguments().getString(KEY_SEED).orEmpty() }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val manager: Manager = playbackManager()
    val binding = FragmentVideosInRecyclerViewBinding.bind(view)
    manager.bucket(binding.videos)

    val engine = Engine.get<StyledPlayerView>(
      manager = manager,
      playableCreator = StyledPlayerViewPlayableCreator.getInstance(view.context),
      rendererProvider = requireActivity().getStyledPlayerViewProvider(),
    )

    val binder = engine.setUp(
      data = VideoUrls.LocalHevc,
      tag = VIDEO_TAG
    )

    binding.videos.withModels {
      /* object : SimpleEpoxyModel(R.layout.holder_video_container) {
        private var requestHandle: RequestHandle? = null

        override fun bind(view: View) {
          super.bind(view)
          val holder = HolderVideoContainerBinding.bind(view)
          requestHandle?.cancel()
          requestHandle = binder
            .copy(tag = "TOP_VIDEO")
            .bind(container = holder.videoContainer)
        }

        override fun unbind(view: View) {
          super.unbind(view)
          requestHandle?.cancel()
        }
      }
        .id(R.layout.holder_video_container)
        .addTo(this) */

      (0 until 8).forEach { index ->
        object : SimpleEpoxyModel(R.layout.holder_video_container) {

          private var requestHandle: RequestHandle? = null

          override fun bind(view: View) {
            super.bind(view)
            val holder = HolderVideoContainerBinding.bind(view)
            requestHandle?.cancel()
            requestHandle = binder
              .withTag(tag = "$seed::$index::RecyclerView")
              .bind(container = holder.videoContainer)
          }

          override fun unbind(view: View) {
            super.unbind(view)
            requestHandle?.cancel()
          }
        }
          .id(index)
          .addTo(this)
      }

      (0 until 8).forEach { index ->
        SimpleEpoxyModel(R.layout.holder_text)
          .id(index + 100)
          .addTo(this)
      }
    }
  }

  companion object {
    private const val KEY_SEED = "KEY_SEED"

    fun getInstance(position: Int): VideosInRecyclerViewFragment = VideosInRecyclerViewFragment()
      .apply {
        arguments = bundleOf(KEY_SEED to "seed::$position")
      }
  }
}
