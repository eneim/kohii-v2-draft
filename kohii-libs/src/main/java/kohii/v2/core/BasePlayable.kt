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
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.Player
import kohii.v2.core.Playable.Command.PAUSED_BY_USER

abstract class BasePlayable<RENDERER : Any>(
  home: Home,
  tag: String,
  data: Any,
  protected val bridge: Bridge<RENDERER>,
  rendererType: Class<RENDERER>,
  firstManager: PlayableManager,
) : AbstractPlayable(
  home = home,
  tag = tag,
  data = data,
  rendererType = rendererType,
  firstManager = firstManager
), Player.Listener {

  override val isStarted: Boolean get() = bridge.isStarted

  override val isPlaying: Boolean get() = bridge.isPlaying

  override val renderer: Any? get() = bridge.renderer

  override fun currentState(): PlayableState = bridge.playableState

  override fun onBind(
    playback: Playback,
    state: PlayableState?
  ) {
    super.onBind(playback, state)
    if (state != null) bridge.playableState = state
  }

  override fun onPrepare(preload: Boolean) {
    super.onPrepare(preload)
    bridge.addPlayerListener(this)
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
    bridge.removePlayerListener(this)
  }

  override fun onRendererDetached(renderer: Any?) {
    super.onRendererDetached(renderer)
    check(bridge.renderer === renderer)
    bridge.renderer = null
  }

  override fun onPlaybackChanged(
    previous: Playback?,
    next: Playback?
  ) {
    super.onPlaybackChanged(previous, next)
    if (previous != null) {
      bridge.removePlayerListener(previous.rawListener)
    }

    if (next != null) {
      bridge.addPlayerListener(next.rawListener)
      bridge.controller = next.controller
    } else {
      bridge.controller = null
    }
  }

  override fun onSaveState(): Bundle = when (val playableState = currentState()) {
    is Parcelable -> bundleOf(PlayableState.KEY_PLAYABLE_STATE to playableState)
    else -> Bundle.EMPTY
  }

  override fun onRestoreState(state: Bundle) {
    when (val savedState: Parcelable? = state.getParcelable(PlayableState.KEY_PLAYABLE_STATE)) {
      is PlayableState -> bridge.playableState = savedState
    }
  }
}
