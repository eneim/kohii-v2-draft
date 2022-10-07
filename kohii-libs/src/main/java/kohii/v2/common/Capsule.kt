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

package kohii.v2.common

/**
 * Singleton Holder
 */
internal open class Capsule<OUTPUT : Any, in INPUT>(
  creator: (INPUT) -> OUTPUT,
  onCreate: ((OUTPUT) -> Unit) = { },
) {
  @Volatile private var instance: OUTPUT? = null

  private var creator: ((INPUT) -> OUTPUT)? = creator
  private var onCreate: ((OUTPUT) -> Unit)? = onCreate

  private fun getInstance(arg: INPUT): OUTPUT = instance ?: synchronized(this) {
    val check = instance
    if (check != null) {
      check
    } else {
      val created = checkNotNull(creator)(arg)
      checkNotNull(onCreate)(created)
      instance = created
      creator = null
      onCreate = null
      created
    }
  }

  @JvmSynthetic
  fun get(arg: INPUT): OUTPUT = getInstance(arg)
}
