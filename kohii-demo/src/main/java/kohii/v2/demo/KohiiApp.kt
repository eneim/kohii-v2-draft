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

package kohii.v2.demo

import android.app.Application
import androidx.fragment.app.Fragment
import com.squareup.moshi.Moshi
import kohii.v2.demo.common.UriJsonAdapter

class KohiiApp : Application() {

  internal val moshi: Moshi = Moshi.Builder()
    .add(UriJsonAdapter)
    .build()
}

internal val Fragment.demoApp: KohiiApp get() = requireActivity().application as KohiiApp
