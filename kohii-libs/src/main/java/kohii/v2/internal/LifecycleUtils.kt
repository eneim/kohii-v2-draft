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

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Copied from Coil.

/** Suspend until [Lifecycle.getCurrentState] is at least [STARTED] */
@JvmSynthetic
@MainThread
internal suspend inline fun Lifecycle.awaitStarted() {
  // Fast path: we're already started.
  if (currentState.isAtLeast(STARTED)) return
  // Slow path: observe the lifecycle until we're started.
  observeStarted()
}

/** Cannot be 'inline' due to a compiler bug. There is a test that guards against this bug. */
@MainThread
private suspend fun Lifecycle.observeStarted() {
  var observer: LifecycleObserver? = null
  try {
    suspendCancellableCoroutine { continuation ->
      observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner): Unit = continuation.resume(Unit)
      }.also(::addObserver)
    }
  } finally {
    // 'observer' will always be null if this method is marked as 'inline'.
    observer?.let(::removeObserver)
  }
}
