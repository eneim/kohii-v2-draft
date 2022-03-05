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

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v2.exoplayer.StyledPlayerViewPlayableCreator
import kohii.v2.exoplayer.StyledPlayerViewProvider

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
 * Creates a new [Engine] that supports the ExoPlayer stack.
 */
@Suppress("FunctionName")
fun Fragment.ExoPlayerEngine(vararg buckets: View): Engine {
  val manager = playbackManager()
  for (bucket in buckets) {
    manager.bucket(bucket)
  }
  return Engine.get<StyledPlayerView>(
    manager = manager,
    playableCreator = StyledPlayerViewPlayableCreator.getInstance(requireContext()),
    rendererProvider = StyledPlayerViewProvider(),
  )
}

/**
 * Creates a new [Engine] that supports the ExoPlayer stack.
 */
@Suppress("FunctionName")
fun Fragment.ExoPlayerEngine(bucket: View): Engine {
  val manager = playbackManager()
  manager.bucket(bucket)
  return Engine.get<StyledPlayerView>(
    manager = manager,
    playableCreator = StyledPlayerViewPlayableCreator.getInstance(requireContext()),
    rendererProvider = StyledPlayerViewProvider(),
  )
}

@Suppress("FunctionName")
fun FragmentActivity.ExoPlayerEngine(bucket: View): Engine {
  val manager = playbackManager()
  manager.bucket(bucket)
  return Engine.get<StyledPlayerView>(
    manager = manager,
    playableCreator = StyledPlayerViewPlayableCreator.getInstance(application),
    rendererProvider = StyledPlayerViewProvider(),
  )
}
