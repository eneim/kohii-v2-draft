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
import kohii.v2.demo.NoVideoInLandscapeScrollViewFragment
import kohii.v2.demo.screens.ads.VideosWithAdsInRecyclerViewFragment
import kohii.v2.demo.screens.chain.ChainedVideosInScrollViewFragment
import kohii.v2.demo.screens.interaction.VideoWithInteractionInScrollViewFragment
import kohii.v2.demo.screens.mixed.MixedVideosInRecyclerViewFragment
import kohii.v2.demo.screens.multiurls.MultiUrlsVideoInScrollViewFragment
import kohii.v2.demo.screens.nested.RecyclerViewInScrollViewFragment
import kohii.v2.demo.screens.recyclerview.VideosInRecyclerViewFragment

class HomeViewModel : ViewModel() {

  val demoItems: LiveData<List<DemoItem>> = MutableLiveData(
    listOf(
      DemoItem(
        title = "Video with interaction in NestedScrollView",
        description = "Demo using NestedScrollView with a single video. Clicking this video will open a dedicated player in a fullscreen dialog. It also force the Activity to landscape.",
        fragment = VideoWithInteractionInScrollViewFragment::class.java,
      ),
      DemoItem(
        title = "Videos in RecyclerView",
        description = "Demo using RecyclerView with many videos.",
        fragment = VideosInRecyclerViewFragment::class.java,
      ),
      DemoItem(
        title = "Many videos in one ViewHolder",
        description = "Demo using RecyclerView with one ViewHolder contains many videos.",
        fragment = MixedVideosInRecyclerViewFragment::class.java,
      ),
      DemoItem(
        title = "Nested RecyclerView",
        description = "Demo with videos inside a RecyclerView that is inside a NestedScrollView.",
        fragment = RecyclerViewInScrollViewFragment::class.java,
      ),
      DemoItem(
        title = "Videos with Ads (ExoPlayer + IMA)",
        description = "Demo with Ads using ExoPlayer and IMA extension.",
        fragment = VideosWithAdsInRecyclerViewFragment::class.java,
      ),
      DemoItem(
        title = "(Experiment) Chained videos in NestedScrollView",
        description = "Demo using NestedScrollView with many videos in a chain.",
        fragment = ChainedVideosInScrollViewFragment::class.java,
      ),
      DemoItem(
        title = "Portrait only video in NestedScrollView",
        description = "Demo using NestedScrollView with videos exist in portrait mode only.",
        fragment = NoVideoInLandscapeScrollViewFragment::class.java,
      ),
      DemoItem(
        title = "Video with multi Urls",
        description = "Demo using a Video that uses a low-res preview url in the scroll view, and a high-res main url in the fullscreen player",
        fragment = MultiUrlsVideoInScrollViewFragment::class.java,
      ),
    )
  )
}
