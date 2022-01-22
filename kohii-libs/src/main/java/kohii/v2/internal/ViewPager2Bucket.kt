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

package kohii.v2.internal

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kohii.v2.core.Axis
import kohii.v2.core.Axis.HORIZONTAL
import kohii.v2.core.Axis.VERTICAL
import kohii.v2.core.Manager
import kohii.v2.core.ViewBucket

internal class ViewPager2Bucket(
  manager: Manager,
  override val rootView: ViewPager2,
) : ViewBucket(manager, rootView) {

  override val axis: Axis
    get() = if (rootView.orientation == RecyclerView.VERTICAL) VERTICAL else HORIZONTAL

  private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageScrolled(
      position: Int,
      positionOffset: Float,
      positionOffsetPixels: Int,
    ) {
      manager.refresh()
    }
    // override fun onPageSelected(position: Int): Unit = manager.refresh()
    // override fun onPageScrollStateChanged(state: Int): Unit = manager.refresh()
  }

  override fun onAdd() {
    super.onAdd()
    rootView.registerOnPageChangeCallback(onPageChangeCallback)
  }

  override fun onRemove() {
    super.onRemove()
    rootView.unregisterOnPageChangeCallback(onPageChangeCallback)
  }
}
