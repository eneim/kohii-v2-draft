/*
 * Copyright (c) 2021. Nam Nguyen, nam@ene.im
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
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.core.util.Pools.Pool
import kohii.v2.BuildConfig

internal fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) {
  "Expected main thread, get: ${Looper.myLooper()?.thread}"
}

/**
 * Acquires all item from the Pool and applies an action for each item.
 */
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

// Because I want to compose the message first, then log it.
@RestrictTo(LIBRARY_GROUP_PREFIX)
internal fun String.logDebug(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.d(tag, this)
  }
}

@RestrictTo(LIBRARY_GROUP_PREFIX)
internal fun String.logInfo(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.i(tag, this)
  }
}

@RestrictTo(LIBRARY_GROUP_PREFIX)
internal fun String.logWarn(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.w(tag, this)
  }
}

@RestrictTo(LIBRARY_GROUP_PREFIX)
internal fun String.logError(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.e(tag, this)
  }
}

internal inline fun debugOnly(action: () -> Unit) {
  if (BuildConfig.DEBUG) action()
}

internal inline fun <T> Iterable<T>.partitionToMutableSets(
  predicate: (T) -> Boolean
): Pair<MutableSet<T>, MutableSet<T>> {
  val first = mutableSetOf<T>()
  val second = mutableSetOf<T>()
  for (element in this) {
    if (predicate(element)) {
      first.add(element)
    } else {
      second.add(element)
    }
  }
  return Pair(first, second)
}

@MainThread
internal inline fun <reified T : Any> View.getTagOrPut(
  key: Int,
  createNew: () -> T
): T = getTag(key) as? T ?: synchronized(this) {
  getTag(key) as? T ?: createNew().also { value -> setTag(key, value) }
}

@MainThread
internal inline fun <reified T : Any> View.getTypedTag(key: Int): T? = getTag(key) as? T
