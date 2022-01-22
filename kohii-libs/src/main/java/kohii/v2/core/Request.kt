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

import android.os.Parcel
import android.os.Parcelable
import com.google.android.exoplayer2.Bundleable
import com.google.android.exoplayer2.MediaItem
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
class Request(
  val data: Any,
  val tag: String? = null,
) : Parcelable {

  @JvmOverloads
  fun copy(tag: String? = null): Request = Request(
    data = this.data,
    tag = tag,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Request

    if (tag != other.tag) return false
    if (data != other.data) return false

    return true
  }

  override fun hashCode(): Int {
    var result = data.hashCode()
    result = 31 * result + (tag?.hashCode() ?: 0)
    return result
  }

  // This Parceler only handles MediaItem.
  internal companion object : Parceler<Request> {
    override fun Request.write(
      parcel: Parcel,
      flags: Int,
    ) {
      if (data is Bundleable) {
        parcel.writeBundle(data.toBundle())
      } else {
        parcel.writeValue(data)
      }
      tag?.let(parcel::writeString)
    }

    override fun create(parcel: Parcel): Request = Request(
      data = try {
        val bundle = parcel.readBundle(MediaItem::class.java.classLoader)
        MediaItem.CREATOR.fromBundle(requireNotNull(bundle))
      } catch (er: Throwable) {
        er.printStackTrace()
        requireNotNull(parcel.readValue(Any::class.java.classLoader))
      },
      tag = parcel.readString(),
    )
  }
}
