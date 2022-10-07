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

package kohii.v2.demo.common

@Suppress("MatchingDeclarationName")
object VideoUrls {

  const val SINTEL_HLS = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8"

  // Video sample with multiple Audios.
  const val SINTEL_MPD = "https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd"

  const val LLAMA_DRAMA_HLS = "https://content.jwplatform.com/manifests/Cl6EVHgQ.m3u8"

  const val LLAMIGOS_MPD = "https://content.jwplatform.com/manifests/Dn90E0Ca.mpd"

  const val LOCAL_BBB_HEVC = "file:///android_asset/media/bbb_hevc_mp3_45s.mp4"
}
