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

package kohii.v2.core

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kohii.v2.exoplayer.PlayerViewPlayableCreator
import kohii.v2.exoplayer.PlayerViewProvider
import kohii.v2.exoplayer.getPlayerViewProvider

fun Fragment.playbackManager(): Manager = Home[requireContext()].registerManagerInternal(
  owner = this,
  managerLifecycleOwner = viewLifecycleOwner,
  managerViewModel = viewModels<Playable.ManagerImpl>()
)

fun ComponentActivity.playbackManager(): Manager = Home[this].registerManagerInternal(
  owner = this,
  managerLifecycleOwner = this,
  managerViewModel = viewModels<Playable.ManagerImpl>()
)

/**
 * Creates a new [Engine] that supports the ExoPlayer stack for this [Fragment].
 */
@Suppress("FunctionName")
@UnstableApi
fun Fragment.ExoPlayerEngine(): Engine {
  val manager = playbackManager()
  return Engine.newInstance<PlayerView>(
    manager = manager,
    playableCreator = PlayerViewPlayableCreator.getInstance(requireContext()),
    rendererProvider = PlayerViewProvider(), // TODO: reusing Activity's PlayerViewProvider?
  )
}

/**
 * Creates a new [Engine] that supports the ExoPlayer stack for this [FragmentActivity].
 */
@Suppress("FunctionName")
@UnstableApi
fun ComponentActivity.ExoPlayerEngine(): Engine {
  val manager = playbackManager()
  return Engine.newInstance<PlayerView>(
    manager = manager,
    playableCreator = PlayerViewPlayableCreator.getInstance(application),
    rendererProvider = getPlayerViewProvider(),
  )
}
