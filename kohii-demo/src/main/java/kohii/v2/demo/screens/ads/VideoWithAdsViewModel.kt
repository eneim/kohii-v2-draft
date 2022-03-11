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

package kohii.v2.demo.screens.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kohii.v2.demo.common.flowWithPrevious
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn

internal class VideoWithAdsViewModel : ViewModel() {

  private val _selectedItem = MutableStateFlow<AdSample?>(null)

  val selectedItem: StateFlow<Pair<AdSample?, AdSample?>> = _selectedItem
    .flowWithPrevious()
    .drop(1)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = Pair(null, null)
    )

  fun setSelection(adSample: AdSample?) {
    _selectedItem.value = adSample
  }
}
