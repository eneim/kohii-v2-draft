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

package kohii.v2.demo.fullscreen

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.Player
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Playback
import kohii.v2.core.PlayerEventListener
import kohii.v2.core.Request
import kohii.v2.demo.R
import kohii.v2.demo.common.FullscreenDialogFragment
import kohii.v2.demo.common.getParcelableCompat
import kohii.v2.demo.common.hideSystemBars
import kohii.v2.demo.databinding.FragmentFullscreenSheetBinding
import kotlin.LazyThreadSafetyMode.NONE

class FullscreenPlayerSheetDialog : FullscreenDialogFragment(R.layout.fragment_fullscreen_sheet) {

  private val request: Request by lazy(NONE) {
    requireNotNull(requireArguments().getParcelableCompat(ARGS_REQUEST))
  }

  private val resultKey: String by lazy(NONE) {
    requireNotNull(requireArguments().getString(ARGS_RESULT_KEY))
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    // Using immersive mode for this dialog.
    dialog?.window?.hideSystemBars()

    val binding: FragmentFullscreenSheetBinding = FragmentFullscreenSheetBinding.bind(view)
    val engine = ExoPlayerEngine()
    engine.useBucket(binding.root)

    engine.setUp(request).bind(container = binding.videoContainer) {
      addPlayerEventListener(object : PlayerEventListener {
        override fun onStateChanged(
          playback: Playback,
          state: Int,
        ) {
          if (state == Player.STATE_ENDED) dismissAllowingStateLoss()
        }
      })
    }
  }

  override fun onCreateDialog(state: Bundle?): Dialog {
    return object : AppCompatDialog(requireContext(), theme) {
      override fun cancel() {
        // Send result back before the View is detached (which triggers the state saving on the
        // Playable. We don't want it to happen, because the playback will be interrupted).
        setFragmentResult(resultKey, bundleOf(ARGS_REQUEST to request))
        super.cancel()
      }
    }
  }

  companion object {

    internal const val ARGS_REQUEST = "ARGS_REQUEST"
    internal const val ARGS_RESULT_KEY = "ARGS_RESULT_KEY"

    fun newInstance(
      request: Request,
      resultKey: String,
    ): FullscreenPlayerSheetDialog = FullscreenPlayerSheetDialog().apply {
      arguments = bundleOf(
        ARGS_REQUEST to request,
        ARGS_RESULT_KEY to resultKey,
      )
    }
  }
}
