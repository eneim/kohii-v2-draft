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

package kohii.v2.demo.screens.multidata

import android.content.pm.ActivityInfo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kohii.v2.core.Request
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MultiUrisVideoViewModel(
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val selectedVideoFlow = MutableStateFlow<Request?>(null)

  val selectedVideoRequest = selectedVideoFlow
    .asStateFlow()

  fun setSelectedRequest(request: Request?) {
    selectedVideoFlow.value = request
  }

  fun getBaseOrientation(): Int =
    savedStateHandle.get<Int>(KEY_ORIENTATION) ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

  fun setBaseOrientationIfAbsent(orientation: Int) {
    if (!savedStateHandle.contains(KEY_ORIENTATION)) {
      savedStateHandle.set(KEY_ORIENTATION, orientation)
    }
  }

  private companion object {
    const val KEY_ORIENTATION = "KEY_ORIENTATION"
  }
}
