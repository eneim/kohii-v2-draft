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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Playback
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentSwitchPlayablesBinding
import kotlin.LazyThreadSafetyMode.NONE

@UnstableApi
@ExperimentalKohiiApi
class RebindPlayablesFragment : Fragment(R.layout.fragment_switch_playables) {

  private val seed: String by lazy(NONE) { requireArguments().getString(KEY_SEED).orEmpty() }

  private var playback: Playback? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentSwitchPlayablesBinding = FragmentSwitchPlayablesBinding.bind(view)
    binding.details.isVisible = false

    val engine = ExoPlayerEngine()
    engine.useBucket(binding.videos)

    // Example: observing Playback flows of a specific tag.
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

    var binders = listOf(
      engine.setUp(data = VideoUrls.LOCAL_BBB_HEVC)
        .withTag(tag = VideoUrls.LOCAL_BBB_HEVC),
      engine.setUp(data = VideoUrls.SINTEL_HLS)
        .withTag(tag = VideoUrls.SINTEL_HLS),
    )

    val containers = listOf(binding.videoTop, binding.videoBottom)

    containers.forEachIndexed { index, playerView ->
      binders[index].bind(playerView)
    }

    binding.switchPlayables.setOnClickListener {
      binders = binders.asReversed()
      containers.forEachIndexed { index, playerView ->
        binders[index].bind(playerView)
      }
    }
  }

  companion object {
    private const val KEY_SEED = "KEY_SEED"

    fun getInstance(position: Int): RebindPlayablesFragment = RebindPlayablesFragment().apply {
      arguments = bundleOf(KEY_SEED to "seed::$position")
    }
  }
}
