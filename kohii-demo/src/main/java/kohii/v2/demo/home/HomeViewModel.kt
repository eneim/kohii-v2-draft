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

package kohii.v2.demo.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kohii.v2.demo.DemoItem
import kohii.v2.demo.NoVideoInLandscapeScrollViewFragment
import kohii.v2.demo.screens.ads.VideosWithAdsInRecyclerViewFragment
import kohii.v2.demo.screens.chain.ChainedVideosInScrollViewFragment
import kohii.v2.demo.screens.mixed.MixedVideosInRecyclerViewFragment

class HomeViewModel : ViewModel() {

  val demoItems: LiveData<List<DemoItem>> = MutableLiveData(
    listOf(
      DemoItem(
        title = "Many videos in one ViewHolder",
        description = "Demo using RecyclerView with one ViewHolder contains many videos.",
        fragment = MixedVideosInRecyclerViewFragment::class.java,
      ),
      DemoItem(
        title = "Videos with Ads (ExoPlayer + IMA)",
        description = "Demo with Ads using ExoPlayer and IMA extension.",
        fragment = VideosWithAdsInRecyclerViewFragment::class.java,
      ),
      DemoItem(
        title = "Chained videos in NestedScrollView",
        description = "Demo using NestedScrollView with many videos in a chain.",
        fragment = ChainedVideosInScrollViewFragment::class.java,
      ),
      DemoItem(
        title = "Portrait only video in NestedScrollView",
        description = "Demo using NestedScrollView with videos exist in portrait mode only.",
        fragment = NoVideoInLandscapeScrollViewFragment::class.java,
      ),
    )
  )
}
