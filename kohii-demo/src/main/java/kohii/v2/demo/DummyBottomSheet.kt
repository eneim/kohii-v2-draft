package kohii.v2.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kohii.v2.demo.databinding.FragmentDummySheetBinding

class DummyBottomSheet : BottomSheetDialogFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding = FragmentDummySheetBinding.inflate(inflater, container, false)
    return binding.root
  }
}
