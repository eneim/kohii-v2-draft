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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Playback
import kohii.v2.core.PlayerEventListener
import kohii.v2.core.Request
import kohii.v2.demo.common.getParcelableCompat
import kohii.v2.demo.common.viewBinding
import kohii.v2.demo.databinding.FragmentDummySheetBinding
import kotlin.LazyThreadSafetyMode.NONE

internal const val EXTRAS_SHEET_STATE = "EXTRAS_SHEET_STATE"
internal const val ARGS_REQUEST = "ARGS_BIND_BUILDER"
internal const val ARGS_RESULT_KEY = "ARGS_RESULT_KEY"

@UnstableApi
@ExperimentalKohiiApi
class DummyBottomSheetDialog : BottomSheetDialogFragment() {

  private val binding: FragmentDummySheetBinding by viewBinding(FragmentDummySheetBinding::bind)

  private val request: Request by lazy(NONE) {
    requireNotNull(requireArguments().getParcelableCompat(ARGS_REQUEST))
  }
  private val resultKey: String by lazy(NONE) {
    requireNotNull(requireArguments().getString(ARGS_RESULT_KEY))
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.fragment_dummy_sheet, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    val engine = ExoPlayerEngine()
    engine.useBucket(binding.root)

    engine
      .setUp(request = request)
      .bind(container = binding.videoContainer) {
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
    val dialog = object : BottomSheetDialog(requireContext(), theme) {
      override fun cancel() {
        // Send result back before the View is detached (which triggers the state saving on the
        // Playable. We don't want it to happen, because the playback will be interrupted).
        setFragmentResult(resultKey, bundleOf(ARGS_REQUEST to request))
        super.cancel()
      }
    }

    val initState = state?.getInt(EXTRAS_SHEET_STATE) ?: STATE_EXPANDED
    dialog.behavior.state = initState
    return dialog
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val sheetState = (dialog as? BottomSheetDialog)?.behavior?.state ?: return
    outState.putInt(EXTRAS_SHEET_STATE, sheetState)
  }

  companion object {

    fun newInstance(
      request: Request,
      resultKey: String,
    ): DummyBottomSheetDialog =
      DummyBottomSheetDialog().apply {
        arguments = bundleOf(
          ARGS_REQUEST to request,
          ARGS_RESULT_KEY to resultKey,
        )
      }
  }
}
