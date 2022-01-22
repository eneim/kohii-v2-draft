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

package kohii.v2.compose

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.core.Engine
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.StyledPlayerViewProvider

@Composable
fun StyledVideoPlayer(
  mediaItems: List<MediaItem>,
  modifier: Modifier = Modifier,
  playerState: PlayerState = rememberPlayerState(),
) {
  val localContext = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current)
  val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
  val localView = LocalView.current
  val manager = remember {
    Home[localContext].registerManagerInternal(
      owner = lifecycleOwner,
      managerLifecycleOwner = lifecycleOwner,
      managerViewModel = ViewModelLazy(
        viewModelClass = Playable.ManagerImpl::class,
        storeProducer = { viewModelStoreOwner.viewModelStore },
        factoryProducer = {
          SavedStateViewModelFactory(
            localContext.applicationContext as Application,
            savedStateRegistryOwner
          )
        }
      )
    )
  }
  manager.bucket(localView)
  val engine = remember {
    Engine.get<StyledPlayerView>(
      manager,
      StyledPlayerViewPlayableCreator.getInstance(localContext),
      StyledPlayerViewProvider(),
    )
  }

  AndroidView(
    modifier = modifier,
    factory = { context ->
      val view = StyledPlayerView(context)
      engine.setUp(mediaItems).bind(view)
      view
    }
  )
}
