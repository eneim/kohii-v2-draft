/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v2.internal

import android.os.Looper
import android.view.View
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.core.util.Pools.Pool

@RestrictTo(LIBRARY_GROUP_PREFIX)
fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) {
  "Expected main thread, get: ${Looper.myLooper()?.thread}"
}

@RestrictTo(LIBRARY_GROUP_PREFIX)
inline fun <T : Any> T?.onNotNull(block: (T) -> Unit) {
  if (this != null) block(this)
}

/**
 * Acquires all item from the Pool and applies an action for each item.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
internal inline fun <T> Pool<T>.onEachAcquired(action: (T) -> Unit) {
  var item: T?
  do {
    item = acquire()
    if (item == null) break
    else action(item)
  } while (true)
}

internal fun <T : Any> T.asString(): String = if (this is View) {
  val original = this.toString()
  original.replace(javaClass.canonicalName.orEmpty(), this.javaClass.simpleName)
} else {
  this.toString()
}

internal fun <T : Any> T.hexCode(): String = Integer.toHexString(hashCode())
