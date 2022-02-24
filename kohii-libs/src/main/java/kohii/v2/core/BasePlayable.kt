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

import android.os.Bundle
import com.google.android.exoplayer2.MediaItem
import kohii.v2.core.Playable.Command.PAUSED_BY_USER
import kohii.v2.core.PlayableState.Companion.toPlayableState

abstract class BasePlayable<RENDERER : Any>(
  home: Home,
  tag: String,
  data: List<MediaItem>,
  protected val bridge: Bridge<RENDERER>,
  rendererType: Class<RENDERER>,
  firstManager: PlayableManager,
) : AbstractPlayable(
  home = home,
  tag = tag,
  data = data,
  rendererType = rendererType,
  firstManager = firstManager
) {

  override val isStarted: Boolean get() = bridge.isStarted && command.get() != PAUSED_BY_USER

  override val isPlaying: Boolean get() = bridge.isPlaying

  override val renderer: Any? get() = bridge.renderer

  override fun currentState(): PlayableState = bridge.playableState

  override fun onPrepare(preload: Boolean) {
    super.onPrepare(preload)
    bridge.prepare(preload)
  }

  override fun onReady() {
    super.onReady()
    bridge.ready()
  }

  override fun onStart() {
    super.onStart()
    if (command.get() != PAUSED_BY_USER) bridge.play()
  }

  override fun onPause() {
    super.onPause()
    bridge.pause()
  }

  override fun onReset() {
    super.onReset()
    bridge.reset()
  }

  override fun onRelease() {
    super.onRelease()
    bridge.release()
  }

  override fun onRendererDetached(renderer: Any?) {
    super.onRendererDetached(renderer)
    if (bridge.renderer === renderer) {
      bridge.renderer = null
    }
  }

  override fun onPlaybackChanged(
    previous: Playback?,
    next: Playback?,
  ) {
    super.onPlaybackChanged(previous, next)
    if (previous != null) {
      bridge.removeComponentsListener(previous.componentsListener)
    }

    if (next != null) {
      bridge.addComponentsListener(next.componentsListener)
      bridge.controller = next.controller
    } else {
      bridge.controller = Controller
    }
  }

  override fun onSaveState(): Bundle = currentState().toBundle()

  override fun onRestoreState(state: Bundle) {
    val savedState: PlayableState? = state.toPlayableState()
    if (savedState != null) bridge.playableState = savedState
  }
}
