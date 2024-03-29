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
import kohii.v2.internal.readArrayListCompat
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
class Request internal constructor(
  val data: List<RequestData>,
  val tag: String? = null,
) : Parcelable {

  /**
   * Creates a new [Request] using the same data with a new tag.
   */
  fun copy(tag: String? = null): Request = Request(
    data = this.data,
    tag = tag,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Request) return false

    if (tag != other.tag) return false
    if (data != other.data) return false

    return true
  }

  override fun hashCode(): Int {
    var result = data.hashCode()
    result = 31 * result + (tag?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "Request(tag=$tag, data=$data)"
  }

  internal companion object : Parceler<Request> {
    override fun Request.write(
      parcel: Parcel,
      flags: Int,
    ) {
      tag?.let(parcel::writeString)
      parcel.writeList(data)
    }

    override fun create(parcel: Parcel): Request = Request(
      tag = parcel.readString(),
      data = parcel.readArrayListCompat() ?: emptyList(),
    )
  }
}
