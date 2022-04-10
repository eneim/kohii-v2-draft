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

package kohii.v2.demo.screens.interaction

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.demo.R
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.common.isAncestorOf
import kohii.v2.demo.databinding.FragmentVideoInScrollViewSimpleBinding
import kohii.v2.demo.fullscreen.FullscreenPlayerActivity.Companion.ARGS_REQUEST
import kohii.v2.demo.fullscreen.FullscreenPlayerActivity.Companion.newPlayerIntent
import kohii.v2.demo.home.DemoItemFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class VideoWithInteractionInScrollViewFragment :
  DemoItemFragment(R.layout.fragment_video_in_scroll_view_simple) {

  private val viewModel: VideoViewModel by viewModels()

  private val startFullscreen = registerForActivityResult(StartActivityForResult()) {
    if (it.resultCode == RESULT_OK && it.data?.hasExtra(ARGS_REQUEST) == true) {
      viewModel.setSelectedRequest(null)
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    if (savedInstanceState == null) {
      viewModel.setBaseOrientationIfAbsent(requireActivity().requestedOrientation)
    }

    val binding = FragmentVideoInScrollViewSimpleBinding.bind(view)

    // Get the correct container as the bucket.
    val bucket = binding.content.takeIf { it.isAncestorOf(binding.video) }
      ?: binding.container
    val engine = ExoPlayerEngine()
    engine.useBucket(bucket)

    val requestTag = VideoUrls.LLAMA_DRAMA_HLS
    val binder = engine.setUp(tag = requestTag, data = VideoUrls.LLAMA_DRAMA_HLS)

    // Opening the video in fullscreen using a (fullscreen) Dialog
    /* viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(State.RESUMED) {
        viewModel.selectedVideoRequest
          .onEach { request ->
            // Remove the Player dialog to address the issue when programmatic orientation change
            // happens: when we reset the Activity orientation back to the "base" one, for some
            // reason the Player dialog is shown again, even if the selected request is null.
            val fullscreenPlayer = childFragmentManager.findFragmentByTag(requestTag)
            if (fullscreenPlayer != null) {
              childFragmentManager.commitNow(allowStateLoss = true) {
                remove(fullscreenPlayer)
              }
            }

            // Note: use requireActivity().requestedOrientation if we want to force the fullscreen player
            // in the landscape mode.
            if (request != null) {
              requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
              FullscreenPlayerSheetDialog.newInstance(request, requestTag)
                .show(childFragmentManager, requestTag)
            } else {
              requireActivity().requestedOrientation = viewModel.getBaseOrientation()
              binder.bind(binding.video)
            }
          }
          .collect()
      }
    } */

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
              startFullscreen.launch(view.context.newPlayerIntent(request))
            } else {
              binder.bind(binding.video)
            }
          }
          .collect()
      }
    }

    childFragmentManager.setFragmentResultListener(requestTag, viewLifecycleOwner) { _, _ ->
      viewModel.setSelectedRequest(null)
    }

    binding.video.setOnClickListener {
      viewModel.setSelectedRequest(binder.request)
    }
  }
}
