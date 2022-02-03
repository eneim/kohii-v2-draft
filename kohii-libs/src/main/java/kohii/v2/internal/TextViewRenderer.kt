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

package kohii.v2.internal

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Range
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_READY
import kohii.v2.core.AbstractPlayable
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableCreator
import kohii.v2.core.PlayableManager
import kohii.v2.core.PlayableState
import kohii.v2.core.PlayableState.Companion.toPlayableState
import kohii.v2.core.PlayableState.Initialized
import kohii.v2.core.PlayableState.Progress
import kohii.v2.core.Playback
import kohii.v2.core.RecycledRendererProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong

private const val DUMMY_LENGTH_MS = (1000 * 60 * 0.5).toLong() /* 5 minutes */

@SuppressLint("SetTextI18n")
class TextViewPlayable(
  home: Home,
  tag: String,
  data: Any,
  firstManager: PlayableManager,
) : AbstractPlayable(
  home = home,
  tag = tag,
  data = data,
  rendererType = TextView::class.java,
  firstManager = firstManager
) {

  override val renderer: TextView? get() = internalRenderer
  override val isStarted: Boolean get() = renderer?.text?.startsWith("START") == true
  override val isPlaying: Boolean get() = renderer?.text?.startsWith("START") == true
  override val triggerRange: Range<Float> = Range(0.0f, 1.0f)

  private val rawPlayerListeners = CopyOnWriteArraySet<Player.Listener>()

  private val timer = AtomicLong(0)
  private var currentState: Int = Player.STATE_IDLE
  private var internalRenderer: TextView? = null
  private var playJob: Job? = null

  init {
    "TextViewPlayable is created".logWarn()
  }

  override fun onSaveState(): Bundle = Progress(
    playerState = STATE_READY,
    totalDurationMillis = DUMMY_LENGTH_MS,
    currentPositionMillis = timer.get(),
    currentMediaItemIndex = 0,
    contentDurationMillis = DUMMY_LENGTH_MS,
    isStarted = true,
    isPlaying = true,
  )
    .toBundle()
    .also { it.putLong("POSITION", timer.get()) }

  override fun onRestoreState(state: Bundle) {
    val position = state.getLong("POSITION", -1).takeIf { it >= 0 }
    if (position != null) {
      timer.set(position)
    } else {
      val playableState: PlayableState? = state.toPlayableState()
      if (playableState is Progress) {
        timer.set(playableState.currentPositionMillis)
      }
    }
  }

  override fun onPlaybackChanged(
    previous: Playback?,
    next: Playback?,
  ) {
    super.onPlaybackChanged(previous, next)
    if (previous != null) rawPlayerListeners.remove(previous.rawListener)
    if (next != null) rawPlayerListeners.add(next.rawListener)
  }

  override fun currentState(): PlayableState = Initialized

  override fun onRendererAttached(renderer: Any?) {
    super.onRendererAttached(renderer)
    internalRenderer = renderer as? TextView
  }

  override fun onRendererDetached(renderer: Any?) {
    super.onRendererDetached(renderer)
    if (internalRenderer === renderer) {
      internalRenderer = null
    }
  }

  override fun onStart() {
    super.onStart()
    if (playJob == null) {
      internalRenderer?.text = "STARTED [$tag]"
      playJob = home.scope.launch {
        if (currentState < STATE_ENDED) {
          reportPlayerState(STATE_BUFFERING)
          delay(500)
          reportPlayerState(STATE_READY)

          while (isActive && timer.get() < DUMMY_LENGTH_MS) {
            timer.addAndGet(200)
            if (timer.get() > DUMMY_LENGTH_MS) timer.set(DUMMY_LENGTH_MS)
            if (timer.get() == 5000L) {
              reportPlayerState(STATE_BUFFERING)
              delay(1000)
            }
            renderer?.text = "STARTED - pos=${timer.get()}"
            delay(200)
          }
        }
        renderer?.text = "ENDED [$tag]"
        reportPlayerState(STATE_ENDED)
      }
    }
  }

  override fun onPause() {
    playJob?.cancel()
    playJob = null
    internalRenderer?.text = "PAUSED [$tag]"
    super.onPause()
  }

  override fun onRelease() {
    super.onRelease()
    timer.set(0)
    internalRenderer?.text = "RELEASED"
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun reportPlayerState(@Player.State state: Int) {
    currentState = state
    if (rawPlayerListeners.isNotEmpty()) {
      for (listener in rawPlayerListeners) {
        listener.onPlaybackStateChanged(state)
      }
    }
  }
}

class TextViewPlayableCreator(val home: Home) : PlayableCreator() {

  override fun createPlayable(
    playableManager: PlayableManager,
    data: Any,
    tag: String,
  ): Playable = TextViewPlayable(
    home = home,
    tag = tag,
    data = data,
    firstManager = playableManager
  )
}

class TextViewRendererProvider(poolSize: Int = 2) : RecycledRendererProvider(poolSize) {

  override fun accept(playback: Playback): Boolean {
    return playback.container is ViewGroup &&
      TextView::class.java.isAssignableFrom(playback.playable.rendererType)
  }

  override fun createRenderer(
    playback: Playback,
    rendererType: Int,
  ): TextView {
    val container = playback.container as ViewGroup
    val result = LayoutInflater.from(container.context)
      .inflate(android.R.layout.simple_list_item_1, container, false) as TextView
    val params = result.layoutParams
    params.width = LayoutParams.WRAP_CONTENT
    params.height = LayoutParams.WRAP_CONTENT
    if (params is FrameLayout.LayoutParams) {
      params.gravity = Gravity.CENTER
    }

    result.layoutParams = params
    result.gravity = Gravity.CENTER
    result.setBackgroundColor(Color.YELLOW)

    "RendererProvider[${hexCode()}]_CREATE [RR=${result.asString()}]".logDebug()
    return result
  }

  override fun recycleRenderer(renderer: Any) {
    "RendererProvider[${hexCode()}]_RECYCLE [RR=${renderer.asString()}]".logDebug()
    (renderer as? TextView)?.text = ""
  }
}
