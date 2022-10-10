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

package kohii.v2.exoplayer

import android.app.Activity
import android.view.View
import androidx.annotation.MainThread
import androidx.media3.common.util.UnstableApi
import kohii.v2.R
import kohii.v2.core.RendererProvider
import kohii.v2.internal.getTagOrPut

/**
 * Returns a [PlayerViewProvider] that is managed by the Activity.
 */
@MainThread @UnstableApi
fun Activity.getPlayerViewProvider(): RendererProvider {
  val rootView: View = checkNotNull(window.peekDecorView()) {
    "Activity's decorView must be available. Please call this method after Activity.onCreate()."
  }

  return rootView.getTagOrPut(R.id.tag_styled_player_view_provider, ::PlayerViewProvider)
}
