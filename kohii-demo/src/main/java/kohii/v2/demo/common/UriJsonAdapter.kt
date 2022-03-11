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

import android.net.Uri
import androidx.core.net.toUri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

class UriJsonAdapter : JsonAdapter<Uri>() {
  override fun fromJson(reader: JsonReader): Uri = reader.nextString().toUri()

  override fun toJson(
    writer: JsonWriter,
    value: Uri?,
  ) {
    writer.value(value.toString())
  }

  override fun toString(): String = "JsonAdapter(Uri)"

  companion object : Factory {
    override fun create(
      type: Type,
      annotations: MutableSet<out Annotation>,
      moshi: Moshi,
    ): JsonAdapter<*>? = if (type == Uri::class.java) UriJsonAdapter() else null
  }
}
