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

package kohii.v2.core

/**
 * An object returned by [Binder.bind] that can be used by the client to cancel the request, or
 * wait for the result.
 */
interface RequestHandle {

  val isCompleted: Boolean

  /**
   * Cancel the on-going request. This method only takes effect if the request is not completed.
   */
  fun cancel()

  /**
   * Await for the result of the on-going request.
   */
  suspend fun result(): Result<Playback>
}
