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
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Engine
import kohii.v2.core.Manager
import kohii.v2.core.playbackManager
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentVideoInScrollViewPortraitOnlyBinding
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.getStyledPlayerViewProvider

class NoVideoInLandscapeScrollViewFragment :
  Fragment(R.layout.fragment_video_in_scroll_view_portrait_only) {

  @OptIn(ExperimentalKohiiApi::class)
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideoInScrollViewPortraitOnlyBinding.bind(view)

    val container = binding.videoContainer
    val bucketView = container?.parent as? ViewGroup
    if (container != null && bucketView != null) {
      val manager: Manager = playbackManager()
      manager.bucket(bucketView)
      val engine = Engine.get<StyledPlayerView>(
        manager = manager,
        playableCreator = StyledPlayerViewPlayableCreator.getInstance(view.context),
        rendererProvider = requireActivity().getStyledPlayerViewProvider(),
      )

      engine.setUp(data = VideoUrls.HlsSample, tag = VIDEO_TAG).bind(container = container)
    }
  }

  companion object {
    internal const val VIDEO_TAG = "VIDEO_TAG"
  }
}
