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

package kohii.v2.demo.screens.basic

import android.os.Bundle
import android.view.View
import androidx.media3.common.util.UnstableApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.demo.R.layout
import kohii.v2.demo.VideoInScrollViewFragment.Companion.VIDEO_TAG
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentVideosInRecyclerViewBinding
import kohii.v2.demo.home.DemoItemFragment

@UnstableApi
class VideosInRecyclerViewFragment : DemoItemFragment(layout.fragment_videos_in_recycler_view) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideosInRecyclerViewBinding.bind(view)
    val engine = ExoPlayerEngine()
    engine.useBucket(binding.videos)

    val binder = engine.setUp(
      tag = VIDEO_TAG,
      data = VideoUrls.LOCAL_BBB_HEVC,
    )

    val adapter = SimpleItemsAdapter(binder = binder, identity = seed)
    binding.videos.adapter = adapter
  }
}
