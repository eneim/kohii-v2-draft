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

import androidx.fragment.app.Fragment
import kotlin.LazyThreadSafetyMode.NONE

abstract class DemoItemFragment : Fragment {

  protected val seed: String by lazy(NONE) { requireArguments().getString(KEY_SEED).orEmpty() }

  constructor() : super()
  constructor(contentLayoutId: Int) : super(contentLayoutId)

  companion object {
    const val KEY_SEED = "KEY_SEED"
  }
}
