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
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.SimpleEpoxyModel
import com.google.android.exoplayer2.MediaItem
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

class VideoInRecyclerViewFragment : Fragment(R.layout.fragment_videos_in_recycler_view) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
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
      data = MediaItem.Builder().setUri(VideoUrls.HlsSample).build(),
      tag = VIDEO_TAG
    )

    binding.videos.withModels {
      object : SimpleEpoxyModel(R.layout.holder_video_container) {
        private var requestHandle: RequestHandle? = null
        override fun bind(view: View) {
          super.bind(view)
          val binding = HolderVideoContainerBinding.bind(view)
          requestHandle?.cancel()
          requestHandle = binder
            .copy(tag = "TOP_VIDEO")
            .bind(container = binding.videoContainer)
        }

        override fun unbind(view: View) {
          super.unbind(view)
          requestHandle?.cancel()
        }
      }
        .id(R.layout.holder_video_container)
        .addTo(this)

      (0 until 20).forEach { index ->
        /* SimpleEpoxyModel(R.layout.holder_text)
          .id(index)
          .addTo(this) */

        object : SimpleEpoxyModel(R.layout.holder_video_container) {
          private var requestHandle: RequestHandle? = null
          override fun bind(view: View) {
            super.bind(view)
            val binding = HolderVideoContainerBinding.bind(view)
            requestHandle?.cancel()
            requestHandle = binder
              .copy(tag = "VIDEO::$index")
              .bind(container = binding.videoContainer)
          }

          override fun unbind(view: View) {
            super.unbind(view)
            requestHandle?.cancel()
          }
        }
          .id(index)
          .addTo(this)
      }
    }
  }
}
