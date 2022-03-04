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
import android.view.ViewGroup
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentVideoInScrollViewPortraitOnlyBinding

class NoVideoInLandscapeScrollViewFragment :
  DemoItemFragment(R.layout.fragment_video_in_scroll_view_portrait_only) {

  @OptIn(ExperimentalKohiiApi::class)
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideoInScrollViewPortraitOnlyBinding.bind(view)

    val container = binding.videoContainer
    val bucketView = container?.parent as? ViewGroup
    if (container != null && bucketView != null) {
      val engine = ExoPlayerEngine(bucket = bucketView)
      engine
        .setUp(data = VideoUrls.LocalHevc, tag = VideoUrls.LocalHevc.toString())
        .bind(container = container)
    }
  }
}
