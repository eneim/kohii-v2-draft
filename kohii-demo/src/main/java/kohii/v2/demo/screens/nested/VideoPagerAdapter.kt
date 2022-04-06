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

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v2.core.Binder
import kohii.v2.demo.common.layoutInflater
import kohii.v2.demo.databinding.HolderVideoBinding
import kohii.v2.demo.screens.recyclerview.SimpleVideoViewHolder

class VideoPagerAdapter(
  private val binder: Binder,
  private val identity: String,
) : Adapter<SimpleVideoViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): SimpleVideoViewHolder = object : SimpleVideoViewHolder(
    HolderVideoBinding.inflate(parent.layoutInflater, parent, false)
  ) {
    init {
      itemView.updateLayoutParams {
        width = MATCH_PARENT
        height = MATCH_PARENT
      }
    }
  }

  override fun onBindViewHolder(
    holder: SimpleVideoViewHolder,
    position: Int,
  ) {
    holder.bind(binder.withTag("$identity::$position"))
  }

  override fun onViewRecycled(holder: SimpleVideoViewHolder) {
    holder.unbind()
  }

  override fun getItemCount(): Int = Int.MAX_VALUE / 2
}
