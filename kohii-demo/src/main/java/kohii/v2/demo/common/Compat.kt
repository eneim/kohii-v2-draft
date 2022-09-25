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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel

@Suppress("DEPRECATION")
inline fun <reified T : Any> Intent.getParcelableExtraCompat(key: String): T? {
  return if (Build.VERSION.SDK_INT >= 33) {
    getParcelableExtra(key, T::class.java)
  } else {
    getParcelableExtra(key) as? T
  }
}

@Suppress("DEPRECATION")
inline fun <reified T : Any> Bundle.getParcelableCompat(key: String): T? {
  return if (Build.VERSION.SDK_INT >= 33) {
    getParcelable(key, T::class.java)
  } else {
    getParcelable(key) as? T
  }
}

@Suppress("DEPRECATION")
inline fun <reified T : Any> Parcel.readArrayListCompat(classLoader: ClassLoader): ArrayList<T>? {
  return if (Build.VERSION.SDK_INT >= 33) {
    readArrayList(classLoader, T::class.java)
  } else {
    readArrayList(classLoader) as? ArrayList<T>
  }
}