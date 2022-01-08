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
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kohii.v2.core.Engine
import kohii.v2.core.Manager
import kohii.v2.core.Playback
import kohii.v2.core.PlayerEventListener
import kohii.v2.core.Request
import kohii.v2.core.playbackManager
import kohii.v2.demo.VideoInScrollViewFragment.Companion.VIDEO_TAG
import kohii.v2.demo.databinding.FragmentDummySheetBinding
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.getStyledPlayerViewProvider
import kotlin.LazyThreadSafetyMode.NONE

class DummyBottomSheetDialog : BottomSheetDialogFragment() {

  private val binding: FragmentDummySheetBinding by viewBinding(FragmentDummySheetBinding::bind)

  private val request: Request by lazy(NONE) {
    requireNotNull(requireArguments().getParcelable(ARGS_REQUEST))
  }

  private var sheetState: Int = BottomSheetBehavior.STATE_COLLAPSED

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_dummy_sheet, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val manager: Manager = playbackManager()
    manager.bucket(binding.root)

    val engine = Engine.get<StyledPlayerView>(
      manager = manager,
      playableCreator = StyledPlayerViewPlayableCreator.getInstance(view.context),
      rendererProvider = requireActivity().getStyledPlayerViewProvider(),
    )

    engine.setUp(request).bind(container = binding.videoContainer) {
      addPlayerEventListener(object : PlayerEventListener {
        override fun onStateChanged(
          playback: Playback,
          state: Int
        ) {
          if (state == Player.STATE_ENDED) dismissAllowingStateLoss()
        }
      })
    }
  }

  override fun onCreateDialog(state: Bundle?): Dialog {
    val dialog = super.onCreateDialog(state)
    if (dialog is BottomSheetDialog) {
      dialog.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(
          bottomSheet: View,
          newState: Int
        ) {
          sheetState = newState
        }

        override fun onSlide(
          bottomSheet: View,
          slideOffset: Float
        ) = Unit
      })

      val initState = state?.getInt(EXTRAS_SHEET_STATE) ?: STATE_EXPANDED
      dialog.behavior.state = initState
    }
    return dialog
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(EXTRAS_SHEET_STATE, sheetState)
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    setFragmentResult(VIDEO_TAG, bundleOf(ARGS_REQUEST to request))
  }

  companion object {

    internal const val EXTRAS_SHEET_STATE = "EXTRAS_SHEET_STATE"
    internal const val ARGS_REQUEST = "ARGS_BIND_BUILDER"

    fun newInstance(request: Request): DummyBottomSheetDialog =
      DummyBottomSheetDialog().apply {
        val args = Bundle()
        args.putParcelable(ARGS_REQUEST, request)
        arguments = args
      }
  }
}
