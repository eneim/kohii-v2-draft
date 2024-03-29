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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kohii.v2.core.Axis
import kohii.v2.core.Axis.HORIZONTAL
import kohii.v2.core.Axis.UNKNOWN
import kohii.v2.core.Axis.VERTICAL
import kohii.v2.core.Manager
import kohii.v2.core.ViewBucket
import kohii.v2.core.fetchContainers
import kohii.v2.core.getContainers
import kohii.v2.core.removeContainers

internal class RecyclerViewBucket(
  manager: Manager,
  override val root: RecyclerView,
) : ViewBucket(
  manager = manager,
  root = root,
) {

  override val axis: Axis
    get() {
      val orientation = with(root.layoutManager) {
        when (this) {
          is LinearLayoutManager -> orientation
          is StaggeredGridLayoutManager -> orientation
          else -> null
        }
      }
      return orientation?.axis ?: UNKNOWN
    }

  private val onScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(
      recyclerView: RecyclerView,
      dx: Int,
      dy: Int,
    ) {
      manager.refresh()
    }
  }

  private val recyclerListener = RecyclerView.RecyclerListener { holder ->
    "Recycle: holder=$holder".logInfo()
    holder.itemView.fetchContainers()
      ?.iterator()
      ?.onRemoveEach { container ->
        "Recycle: container=$container".logDebug()
        removeContainer(container)
      }

    holder.itemView.removeContainers()
  }

  override fun onAdd() {
    super.onAdd()
    root.addOnScrollListener(onScrollListener)
    root.addRecyclerListener(recyclerListener)
  }

  override fun onRemove() {
    super.onRemove()
    root.removeRecyclerListener(recyclerListener)
    root.removeOnScrollListener(onScrollListener)
  }

  override fun addContainer(container: Any) {
    super.addContainer(container)
    if (container is View) {
      root.findContainingItemView(container)
        ?.getContainers()
        ?.add(container)
    }
  }

  override fun removeContainer(container: Any) {
    super.removeContainer(container)
    if (container is View) {
      root.findContainingItemView(container)
        ?.fetchContainers()
        ?.remove(container)
    }
  }
}

private val Int.axis: Axis get() = if (this == RecyclerView.VERTICAL) VERTICAL else HORIZONTAL
