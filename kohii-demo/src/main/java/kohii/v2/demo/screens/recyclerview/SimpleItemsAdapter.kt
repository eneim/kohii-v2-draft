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

package kohii.v2.demo.screens.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v2.core.Binder
import kohii.v2.demo.common.CommonViewHolder
import kohii.v2.demo.common.layoutInflater
import kohii.v2.demo.databinding.HolderTextBinding
import kohii.v2.demo.databinding.HolderVideoBinding

class SimpleItemsAdapter(
  private val binder: Binder,
  private val identity: String,
) : Adapter<CommonViewHolder<*>>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): CommonViewHolder<*> {
    return if (viewType == TYPE_VIDEO) {
      SimpleVideoViewHolder(
        HolderVideoBinding.inflate(parent.layoutInflater, parent, false)
      )
    } else {
      TextViewHolder(
        HolderTextBinding.inflate(parent.layoutInflater, parent, false)
      )
    }
  }

  override fun onBindViewHolder(
    holder: CommonViewHolder<*>,
    position: Int,
  ) {
    holder.bind(binder.withTag("$identity::$position"))
  }

  override fun onViewRecycled(holder: CommonViewHolder<*>) {
    holder.unbind()
  }

  override fun onFailedToRecycleView(holder: CommonViewHolder<*>): Boolean = true

  override fun getItemCount(): Int = Int.MAX_VALUE / 2

  override fun getItemViewType(position: Int): Int =
    if (position % 3 == 0) TYPE_VIDEO else TYPE_TEXT

  private companion object {

    const val TYPE_VIDEO = 1
    const val TYPE_TEXT = 2
  }
}
