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

package kohii.v2.demo.screens.multiurls

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Playback
import kohii.v2.core.PlayerEventListener
import kohii.v2.demo.R
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.common.isAncestorOf
import kohii.v2.demo.databinding.FragmentVideoInScrollViewSimpleBinding
import kohii.v2.demo.fullscreen.FullscreenPlayerActivity.Companion.ARGS_REQUEST
import kohii.v2.demo.home.DemoItemFragment
import kohii.v2.demo.screens.multiurls.MainVideoPlayerActivity.Companion.newIntent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@UnstableApi
class MultiUrlsVideoInScrollViewFragment :
  DemoItemFragment(R.layout.fragment_video_in_scroll_view_simple) {

  private val viewModel: MultiUrlsVideoViewModel by viewModels()

  private val startFullscreen = registerForActivityResult(StartActivityForResult()) {
    if (it.resultCode == RESULT_OK && it.data?.hasExtra(ARGS_REQUEST) == true) {
      viewModel.setSelectedRequest(null)
    }
  }

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    if (savedInstanceState == null) {
      viewModel.setBaseOrientationIfAbsent(requireActivity().requestedOrientation)
    }

    val binding = FragmentVideoInScrollViewSimpleBinding.bind(view)
    binding.longText1.text = "Preview video using 270p source."

    // Get the correct container as the bucket.
    val bucket = binding.content.takeIf { it.isAncestorOf(binding.video) } ?: binding.container
    val engine = ExoPlayerEngine()
    engine.useBucket(bucket)

    val requestTag = VideoUrls.LLAMA_DRAMA_HLS
    val previewData = PreviewVideoData(
      previewUrl = "https://content.jwplatform.com/videos/Cl6EVHgQ-AZtqUUiX.mp4", // 270p
      mainUrl = "https://content.jwplatform.com/videos/Cl6EVHgQ-TkIjsDEe.mp4" // 1080p
    )
    val mainData = MainVideoData(
      previewUrl = "https://content.jwplatform.com/videos/Cl6EVHgQ-AZtqUUiX.mp4", // 270p
      mainUrl = "https://content.jwplatform.com/videos/Cl6EVHgQ-TkIjsDEe.mp4" // 1080p
    )

    val previewBinder = engine.setUp(tag = requestTag, data = previewData)
    val mainBinder = engine.setUp(tag = requestTag, data = mainData)

    // Opening the video in fullscreen using an Activity.
    //
    // Note: we need to use `repeatOnLifecycle` with `State.RESUMED` to consume the latest value
    // of `viewModel.selectedVideoRequest` after this Fragment is resumed. The reason is: after
    // starting the fullscreen Activity and configuration change happens, if we end the fullscreen
    // Activity, the ActivityResultCallback is called when this Fragment is started, but at the
    // same time, the viewModel LiveData starts sending the data to its Observers. As a result,
    // the Observer receives the old Request (before the ActivityResultCallback is called) and it
    // opens the fullscreen Activity again.
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(State.RESUMED) {
        viewModel.selectedVideoRequest
          .onEach { request ->
            if (request != null) {
              startFullscreen.launch(view.context.newIntent(request))
            } else {
              previewBinder.bind(binding.video) {
                addPlayerEventListener(object : PlayerEventListener {
                  override fun onVideoSizeChanged(
                    playback: Playback,
                    videoSize: VideoSize,
                  ) {
                    binding.longText1.text =
                      "Preview video size: ${videoSize.width} × ${videoSize.height}"
                  }
                })
              }
            }
          }
          .collect()
      }
    }

    binding.video.setOnClickListener {
      viewModel.setSelectedRequest(mainBinder.request)
    }
  }
}
