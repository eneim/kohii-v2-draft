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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kohii.v2.core.Playback
import kohii.v2.core.Request
import kohii.v2.core.RequestHandle
import kotlinx.coroutines.Deferred

/**
 * An implementation of the [RequestHandle] that automatically remove the request once it is done,
 * or the lifecycle that requested this is destroyed.
 */
internal class RequestHandleImpl(
  override val request: Request,
  internal val lifecycle: Lifecycle,
  private val deferred: Deferred<Result<Playback>>,
) : RequestHandle, DefaultLifecycleObserver {

  override val isCompleted: Boolean get() = deferred.isCompleted

  override fun onDestroy(owner: LifecycleOwner): Unit = cancel()

  override fun cancel(): Unit = deferred.cancel()

  override suspend fun result(): Result<Playback> = deferred.await()
}
