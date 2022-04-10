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

package kohii.v2.demo.screens.basic

import kohii.v2.core.Binder
import kohii.v2.core.RequestHandle
import kohii.v2.demo.common.CommonViewHolder
import kohii.v2.demo.databinding.HolderVideoBinding

open class SimpleVideoViewHolder(binding: HolderVideoBinding) :
  CommonViewHolder<HolderVideoBinding>(binding) {

  private var handle: RequestHandle? = null

  override fun bind(data: Any?) {
    super.bind(data)
    handle?.cancel()
    if (data is Binder) {
      handle = data.bind(binding.video)
    }
  }

  override fun unbind() {
    handle?.cancel()
    handle = null
  }
}
