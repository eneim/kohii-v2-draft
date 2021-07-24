package kohii.v2.common

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import kohii.v2.BuildConfig

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

internal inline fun <T, R> Iterable<T>.partitionToMutableSets(
  predicate: (T) -> Boolean,
  transform: (T) -> R
): Pair<MutableSet<R>, MutableSet<R>> {
  val first = mutableSetOf<R>()
  val second = mutableSetOf<R>()
  for (element in this) {
    if (predicate(element)) {
      first.add(transform(element))
    } else {
      second.add(transform(element))
    }
  }
  return Pair(first, second)
}
