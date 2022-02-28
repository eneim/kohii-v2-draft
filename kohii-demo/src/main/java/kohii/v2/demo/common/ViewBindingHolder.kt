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

package kohii.v2.demo.common

import android.view.View
import androidx.viewbinding.ViewBinding
import com.airbnb.epoxy.EpoxyHolder

abstract class ViewBindingHolder<VB : ViewBinding>(
  private val bindingCreator: (View) -> VB,
) : EpoxyHolder() {

  private lateinit var _binding: VB

  val binding: VB get() = _binding

  override fun bindView(itemView: View) {
    _binding = bindingCreator(itemView)
  }
}
