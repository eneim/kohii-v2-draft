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
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Playback
import kohii.v2.core.Request
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.common.getParcelableCompat
import kohii.v2.demo.databinding.FragmentSwitchPlayablesBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicInteger
import kotlin.LazyThreadSafetyMode.NONE

@UnstableApi
@ExperimentalKohiiApi
class SwitchPlayablesFragment : Fragment(R.layout.fragment_switch_playables) {

  private var playback: Playback? = null

  private val seed: String by lazy(NONE) { requireArguments().getString(KEY_SEED).orEmpty() }
  private val commonTag: String by lazy(NONE) { "$seed::${VideoUrls.LOCAL_BBB_HEVC}::Switch" }
  private val commonData = VideoUrls.LOCAL_BBB_HEVC

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentSwitchPlayablesBinding = FragmentSwitchPlayablesBinding.bind(view)

    val engine = ExoPlayerEngine()
    engine.useBucket(binding.videos)

    // Example: observing Playback flows of a specific tag.
    engine.manager.getPlaybackFlow(tag = commonTag)
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

    val binder = engine.setUp(data = commonData)
      .withTag(tag = commonTag)
    val containers = listOf(binding.videoTop, binding.videoBottom)

    val fullScreenListener = PlayerView.FullscreenButtonClickListener { isFullScreen ->
      if (isFullScreen) {
        DummyBottomSheetDialog.newInstance(binder.request, commonTag)
          .show(childFragmentManager, commonTag)
      }
    }

    containers.forEach { playerView ->
      playerView.setFullscreenButtonClickListener(fullScreenListener)
    }

    val index = AtomicInteger(0)

    binding.switchPlayables.setOnClickListener {
      binder.bind(containers[index.getAndIncrement() % containers.size])
    }

    binder.bind(containers[index.getAndIncrement() % containers.size])

    childFragmentManager
      .setFragmentResultListener(commonTag, viewLifecycleOwner) { _, bundle ->
        val request: Request = requireNotNull(bundle.getParcelableCompat(ARGS_REQUEST))
        engine
          .setUp(request)
          .bind(containers[index.getAndIncrement() % containers.size])
      }
  }

  companion object {
    private const val KEY_SEED = "KEY_SEED"

    fun getInstance(position: Int): SwitchPlayablesFragment = SwitchPlayablesFragment().apply {
      arguments = bundleOf(KEY_SEED to "seed::$position")
    }
  }
}
