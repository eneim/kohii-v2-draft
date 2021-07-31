package kohii.v2.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kohii.v2.core.Home
import kohii.v2.demo.databinding.FragmentContentBinding
import kotlinx.coroutines.flow.collect

class SimpleContentFragment : Fragment(R.layout.fragment_content) {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentContentBinding = FragmentContentBinding.bind(view)

    val home: Home = Home[view.context]
    val manager = home.register(this)

    val leftBuilder = home.setUp(MainActivity.DemoUrl) {
      withPlayableTag("左")
    }

    val rightBuilder = home.setUp(MainActivity.DemoUrl) {
      withPlayableTag("右")
    }

    val leftBind = manager.bucket(binding.videos)
      .bind(leftBuilder, binding.containerTop)

    val rightBind = manager.bucket(binding.videos)
      .bind(rightBuilder, binding.containerBottom)

    viewLifecycleOwner.lifecycleScope.launchWhenCreated {
      val leftPlayback = leftBind.result().getOrNull()
      leftPlayback?.playableStateFlow?.collect {
        Log.i("Kohii", "[LEFT] Flow: $it")
      }
    }

    viewLifecycleOwner.lifecycleScope.launchWhenCreated {
      val rightPlayback = rightBind.result().getOrNull()
      rightPlayback?.playableStateFlow?.collect {
        Log.d("Kohii", "[RIGHT] Flow: $it")
      }
    }
  }
}
