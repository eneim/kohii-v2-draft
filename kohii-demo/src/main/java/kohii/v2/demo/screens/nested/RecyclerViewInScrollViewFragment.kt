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

package kohii.v2.demo.screens.nested

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.Carousel.SnapHelperFactory
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.demo.R
import kohii.v2.demo.VideoInScrollViewFragment
import kohii.v2.demo.common.VideoUrls
import kohii.v2.demo.databinding.FragmentCarouselInScrollviewBinding
import kohii.v2.demo.home.DemoItemFragment

@UnstableApi
class RecyclerViewInScrollViewFragment :
  DemoItemFragment(R.layout.fragment_carousel_in_scrollview) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    Carousel.setDefaultGlobalSnapHelperFactory(object : SnapHelperFactory() {
      override fun buildSnapHelper(context: Context?): SnapHelper = PagerSnapHelper()
    })

    val binding = FragmentCarouselInScrollviewBinding.bind(view)
    val engine = ExoPlayerEngine()
    engine.useBuckets(
      binding.container,
      binding.firstCarousel,
      binding.secondCarousel
    )

    val binder = engine.setUp(data = VideoUrls.LOCAL_BBB_HEVC)
      .withTag(tag = VideoInScrollViewFragment.VIDEO_TAG)

    binding.firstCarousel.adapter = VideoPagerAdapter(
      binder = binder,
      identity = "Carousel#1"
    )

    binding.secondCarousel.adapter = VideoPagerAdapter(
      binder = binder,
      identity = "Carousel#2"
    )
  }
}
