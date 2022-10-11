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
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Request
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.common.getParcelableCompat
import kohii.v2.demo.databinding.FragmentVideoInScrollViewBinding

@UnstableApi
class VideoInScrollViewFragment : Fragment(R.layout.fragment_video_in_scroll_view) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentVideoInScrollViewBinding.bind(view)

    //region Setup the Engine
    val engine = ExoPlayerEngine()
    if (binding.videoContainer.parent == binding.videoArea) {
      engine.useBucket(binding.videoArea ?: binding.content)
    } else {
      engine.useBucket(binding.videos)
    }
    //endregion

    setFragmentResultListener(VIDEO_TAG) { _, bundle ->
      val request: Request = requireNotNull(bundle.getParcelableCompat(ARGS_REQUEST))
      engine.setUp(request).bind(container = binding.videoContainer)
      clearFragmentResult(VIDEO_TAG)
    }

    val binder = engine.setUp(
      tag = VIDEO_TAG,
      data = VideoUrls.SINTEL_HLS,
    )

    val lazyPlayback = binder.bind(container = binding.videoContainer)

    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
      val playback = lazyPlayback.result().getOrNull() ?: return@launchWhenResumed

      binding.startButton.setOnClickListener {
        playback.controller.play()
      }

      binding.pauseButton.setOnClickListener {
        playback.controller.pause()
      }
    }

    /* binding.videoContainer.setOnClickListener {
      DummyBottomSheetDialog.newInstance(binder.request)
        .show(parentFragmentManager, "$binder")
    } */
  }

  companion object {
    internal const val VIDEO_TAG = "VIDEO_TAG"
  }
}
