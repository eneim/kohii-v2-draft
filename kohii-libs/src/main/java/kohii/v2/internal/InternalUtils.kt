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

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcel
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.collection.SparseArrayCompat
import androidx.core.util.Pools.Pool
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import kohii.v2.BuildConfig

internal fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) {
  "Expected main thread, get: ${Looper.myLooper()?.thread}"
}

/**
 * Acquires all item from the Pool and applies an action for each item.
 */
@JvmSynthetic
internal inline fun <T> Pool<T>.onEachAcquired(action: (T) -> Unit) {
  do {
    val item = acquire()
    if (item == null) break
    else action(item)
  } while (true)
}

@JvmSynthetic
internal fun <T : Any> T.asString(): String = if (this is View) {
  val original = this.toString()
  original.replace(javaClass.canonicalName.orEmpty(), this.javaClass.simpleName)
} else {
  this.toString()
}

@JvmSynthetic
internal fun <T : Any> T.hexCode(): String = Integer.toHexString(hashCode())

// Because I want to compose the message first, then log it.
@JvmSynthetic
internal fun String.logDebug(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.d(tag, this)
  }
}

@JvmSynthetic
internal fun String.logInfo(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.i(tag, this)
  }
}

@JvmSynthetic
internal fun String.logWarn(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.w(tag, this)
  }
}

@JvmSynthetic
internal fun String.logError(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.e(tag, this)
  }
}

@JvmSynthetic
internal fun String.logStackTrace(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.w(tag, Log.getStackTraceString(Throwable(this)))
  }
}

@JvmSynthetic
internal inline fun debugOnly(action: () -> Unit) {
  if (BuildConfig.DEBUG) action()
}

@JvmSynthetic
internal inline fun <T> Iterable<T>.partitionToMutableSets(
  predicate: (T) -> Boolean,
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

/**
 * Removes all item from this object, and applies [action] on each item after it is removed.
 */
@JvmSynthetic
internal inline fun <T> MutableIterator<T>.onRemoveEach(action: (T) -> Unit) {
  while (hasNext()) {
    val target = next()
    remove()
    action(target)
  }
}

@JvmSynthetic
internal inline fun <T> MutableCollection<T>.onRemoveEach(action: (T) -> Unit) =
  iterator().onRemoveEach(action)

@MainThread @JvmSynthetic
internal inline fun <reified T : Any> View.getTagOrPut(
  key: Int,
  createNew: () -> T,
): T = getTag(key) as? T ?: synchronized(this) {
  getTag(key) as? T ?: createNew().also { value -> setTag(key, value) }
}

@MainThread @JvmSynthetic
internal inline fun <reified T : Any> View.getTypedTag(key: Int): T? = getTag(key) as? T

@JvmSynthetic
internal inline fun Player.doOnTracksChanged(
  crossinline action: Player.(Tracks) -> Unit,
) = addListener(object : Player.Listener {
  override fun onTracksChanged(tracks: Tracks) {
    removeListener(this)
    action(tracks)
  }
})

@JvmSynthetic
inline fun <reified T : Any> Bundle.getParcelableCompat(key: String): T? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU /* 33 */) {
    getParcelable(key, T::class.java)
  } else {
    @Suppress("DEPRECATION")
    getParcelable(key) as? T
  }
}

@JvmSynthetic @Suppress("DEPRECATION")
internal inline fun <reified T : Any> Parcel.readArrayListCompat(classLoader: ClassLoader?): ArrayList<T>? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU /* 33 */) {
    readArrayList(classLoader, T::class.java)
  } else {
    @Suppress("UNCHECKED_CAST")
    readArrayList(classLoader) as? ArrayList<T>
  }
}

@JvmSynthetic
internal inline fun <reified T : Any> Parcel.readParcelableCompat(): T? {
  val clazz = T::class.java
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU /* 33 */) {
    readParcelable(clazz.classLoader, clazz)
  } else {
    @Suppress("DEPRECATION")
    readParcelable(clazz.classLoader)
  }
}

@JvmSynthetic
internal inline fun <T> SparseArrayCompat<T>.getOrPut(
  key: Int,
  defaultValue: () -> T,
) = get(key) ?: defaultValue().also { put(key, it) }
