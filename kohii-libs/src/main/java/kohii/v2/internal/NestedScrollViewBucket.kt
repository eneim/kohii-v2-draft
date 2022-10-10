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

import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import kohii.v2.core.Axis
import kohii.v2.core.Axis.VERTICAL
import kohii.v2.core.Manager
import kohii.v2.core.ViewBucket

class NestedScrollViewBucket(
  manager: Manager,
  override val rootView: NestedScrollView,
) : ViewBucket(
  manager,
  rootView
), OnScrollChangeListener {

  override val axis: Axis = VERTICAL

  override fun onScrollChange(
    v: NestedScrollView,
    scrollX: Int,
    scrollY: Int,
    oldScrollX: Int,
    oldScrollY: Int,
  ) {
    manager.refresh()
  }

  override fun onAdd() {
    super.onAdd()
    rootView.setOnScrollChangeListener(this)
  }

  override fun onRemove() {
    super.onRemove()
    rootView.setOnScrollChangeListener(null as OnScrollChangeListener?)
  }
}
