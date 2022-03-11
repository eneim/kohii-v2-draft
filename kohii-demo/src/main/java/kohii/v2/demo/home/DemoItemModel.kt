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

import android.view.View
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyAttribute.Option.DoNotHash
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import kohii.v2.demo.R
import kohii.v2.demo.common.ViewBindingHolder
import kohii.v2.demo.databinding.HolderDemoItemBinding
import kohii.v2.demo.home.DemoItemModel.Holder

@EpoxyModelClass
abstract class DemoItemModel : EpoxyModelWithHolder<Holder>() {

  override fun getDefaultLayout(): Int = R.layout.holder_demo_item

  @EpoxyAttribute
  internal lateinit var data: DemoItem

  @EpoxyAttribute(DoNotHash)
  var onClick: ((View, DemoItem) -> Unit)? = null

  override fun bind(holder: Holder) {
    super.bind(holder)
    holder.binding.root.setOnClickListener {
      onClick?.invoke(it, data)
    }
    holder.binding.title.text = data.title
    holder.binding.description.text = data.description
  }

  override fun unbind(holder: Holder) {
    super.unbind(holder)
    holder.binding.root.setOnClickListener(null)
    holder.binding.title.text = null
    holder.binding.description.text = null
  }

  class Holder : ViewBindingHolder<HolderDemoItemBinding>(HolderDemoItemBinding::bind)
}
