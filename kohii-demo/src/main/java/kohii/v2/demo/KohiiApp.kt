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

package kohii.v2.demo

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson

class KohiiApp : Application() {

  internal val moshi: Moshi = Moshi.Builder()
    .add(object : JsonAdapter<Uri>() {
      @FromJson
      override fun fromJson(reader: JsonReader): Uri = reader.nextString().toUri()

      @ToJson
      override fun toJson(
        writer: JsonWriter,
        value: Uri?,
      ) {
        writer.value(value.toString())
      }

      override fun toString(): String = "JsonAdapter(Uri)"
    })
    .build()
}

val Fragment.demoApp: KohiiApp get() = requireActivity().application as KohiiApp
