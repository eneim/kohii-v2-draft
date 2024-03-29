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

package kohii.v2.demo.screens.chain

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Chain.SelectScope.AVAILABLE_ONLY
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.demo.R.layout
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentVideosInScrollViewBinding
import kohii.v2.demo.home.DemoItemFragment

@UnstableApi
@ExperimentalKohiiApi
class ChainedVideosInScrollViewFragment : DemoItemFragment(layout.fragment_videos_in_scroll_view) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideosInScrollViewBinding.bind(view)

    val engine = ExoPlayerEngine()
    val bucket = engine.useBucket(binding.videos)

    engine.setUp(data = VideoUrls.LOCAL_BBB_HEVC)
      .withTag("Z")
      .bind(binding.topVideo)

    bucket.chain(loop = true, selectScope = AVAILABLE_ONLY) {
      listOf(
        "A" to binding.firstVideo,
        "B" to binding.secondVideo,
        "C" to binding.thirdVideo
      )
        .map { (tag, container) ->
          Triple(
            tag,
            container,
            engine.setUp(data = VideoUrls.LOCAL_BBB_HEVC)
              .withTag(tag)
              .bind(container)
          )
        }
        .onEach { (_, container, handle) ->
          viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val playback = handle.result().getOrNull() ?: return@launchWhenCreated
            container.setOnClickListener {
              playback.controller.play()
            }
          }
        }
        .forEach { (tag, _, _) -> addPlaybackTag(tag) }
    }
  }
}
