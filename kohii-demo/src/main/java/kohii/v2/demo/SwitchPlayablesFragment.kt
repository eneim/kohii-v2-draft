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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ui.StyledPlayerControlView.OnFullScreenModeChangedListener
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Engine
import kohii.v2.core.Manager
import kohii.v2.core.Playback
import kohii.v2.core.Request
import kohii.v2.core.playbackManager
import kohii.v2.demo.DummyBottomSheetDialog.Companion.ARGS_REQUEST
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentSwitchPlayablesBinding
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.getStyledPlayerViewProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicInteger
import kotlin.LazyThreadSafetyMode.NONE

@ExperimentalKohiiApi
class SwitchPlayablesFragment : Fragment(R.layout.fragment_switch_playables) {

  private var playback: Playback? = null

  private val seed: String by lazy(NONE) { requireArguments().getString(KEY_SEED).orEmpty() }
  private val commonTag: String by lazy(NONE) { "$seed::${VideoUrls.LOCAL_BBB_HEVC}::Switch" }
  private val commonData = VideoUrls.LOCAL_BBB_HEVC

  @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val manager: Manager = playbackManager()
    val binding: FragmentSwitchPlayablesBinding = FragmentSwitchPlayablesBinding.bind(view)
    manager.bucket(binding.videos)

    val engine = Engine.get<StyledPlayerView>(
      manager = manager,
      playableCreator = StyledPlayerViewPlayableCreator.getInstance(requireContext()),
      rendererProvider = requireActivity().getStyledPlayerViewProvider(),
    )

    // Example: observing Playback flows of a specific tag.
    manager.getPlaybackFlow(tag = commonTag)
      .onEach { playback: Playback? ->
        this.playback = playback
        binding.details.text = "Playback: $playback"
      }
      .launchIn(viewLifecycleOwner.lifecycleScope)

    binding.removeAll.setOnClickListener {
      viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        playback?.unbind()
      }
    }

    binding.toggle.setOnClickListener {
      val playback = this.playback ?: return@setOnClickListener
      if (playback.isStarted) {
        playback.controller.pause()
      } else {
        playback.controller.play()
      }
    }

    val binder = engine.setUp(tag = commonTag, data = commonData)
    val containers = listOf(binding.videoTop, binding.videoBottom)

    val fullScreenListener = OnFullScreenModeChangedListener { isFullScreen ->
      if (isFullScreen) {
        DummyBottomSheetDialog.newInstance(binder.request, commonTag)
          .show(childFragmentManager, commonTag)
      }
    }

    containers.forEach { playerView ->
      playerView.setControllerOnFullScreenModeChangedListener(fullScreenListener)
    }

    val index = AtomicInteger(0)

    binding.switchPlayables.setOnClickListener {
      binder.bind(containers[index.getAndIncrement() % containers.size])
    }

    binder.bind(containers[index.getAndIncrement() % containers.size])

    childFragmentManager
      .setFragmentResultListener(commonTag, viewLifecycleOwner) { resultKey, bundle ->
        val request: Request = requireNotNull(bundle.getParcelable(ARGS_REQUEST))
        engine.setUp(request).bind(containers[index.getAndIncrement() % containers.size])
        childFragmentManager.clearFragmentResult(resultKey)
      }
  }

  companion object {
    private const val KEY_SEED = "KEY_SEED"

    fun getInstance(position: Int): SwitchPlayablesFragment = SwitchPlayablesFragment().apply {
      arguments = bundleOf(KEY_SEED to "seed::$position")
    }
  }
}
