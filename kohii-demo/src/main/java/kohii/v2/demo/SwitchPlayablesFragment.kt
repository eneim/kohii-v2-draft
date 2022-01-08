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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.core.Engine
import kohii.v2.core.Manager
import kohii.v2.core.Playback
import kohii.v2.core.playbackManager
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentSwitchPlayablesBinding
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.getStyledPlayerViewProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicInteger

class SwitchPlayablesFragment : Fragment(R.layout.fragment_switch_playables) {

  private var playback: Playback? = null

  @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
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
    manager.getPlaybackFlow(VideoUrls.LocalVP9)
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

    val containers = listOf(binding.videoTop, binding.videoBottom)
    val index = AtomicInteger(0)

    binding.switchPlayables.setOnClickListener {
      engine.setUp(VideoUrls.LocalVP9, tag = VideoUrls.LocalVP9)
        .bind(containers[index.getAndIncrement() % containers.size])
    }

    engine.setUp(VideoUrls.LocalVP9, tag = VideoUrls.LocalVP9)
      .bind(containers[index.getAndIncrement() % containers.size])
  }
}
