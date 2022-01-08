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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import kohii.v2.core.playbackManager
import kohii.v2.demo.databinding.FragmentViewpager2Binding

class ViewPager2Fragment : Fragment(R.layout.fragment_viewpager_2) {

  class PagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
  ) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
      return if (position == 0) {
        // VideosInScrollViewFragment()
        // VideoInRecyclerViewFragment()
        // VideoInScrollViewFragment()
        SwitchPlayablesFragment()
      } else {
        TextInScrollViewFragment()
      }
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding = FragmentViewpager2Binding.bind(view)
    playbackManager().bucket(binding.pager)
    binding.pager.adapter = PagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
  }
}
