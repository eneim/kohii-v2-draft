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

import android.os.Handler
import android.os.Looper
import android.os.Message
import kohii.v2.core.Group
import kohii.v2.core.Playable.Command.PAUSED_BY_USER
import kohii.v2.core.Playback
import kotlin.system.measureNanoTime

internal class GroupDispatcher(
  private val group: Group,
  looper: Looper,
) : Handler(looper) {

  fun dispatchRefresh() {
    removeMessages(MSG_REFRESH)
    sendEmptyMessageDelayed(MSG_REFRESH, DEFAULT_DELAY_MS)
  }

  fun dispatchStartPlayback(
    playback: Playback,
    delay: Long = 0,
  ) {
    removeMessages(MSG_START_PLAYBACK, playback)
    if (playback.playable.command.get() == PAUSED_BY_USER) {
      pausePlayback(playback)
    } else {
      if (delay <= 0) {
        obtainMessage(MSG_START_PLAYBACK, playback).sendToTarget()
      } else {
        sendMessageDelayed(
          obtainMessage(MSG_START_PLAYBACK, playback),
          delay
        )
      }
    }
  }

  fun dispatchPausePlayback(playback: Playback) {
    removeMessages(MSG_START_PLAYBACK, playback)
    pausePlayback(playback)
  }

  fun onDestroy(): Unit = removeCallbacksAndMessages(null)

  private fun startPlayback(playback: Playback) {
    check(playback.isAdded) { "$playback is not added." }
    group.home.startPlayable(playback.playable)
  }

  private fun pausePlayback(playback: Playback) {
    check(playback.isAdded) { "$playback is not added." }
    group.home.pausePlayable(playback.playable)
  }

  override fun handleMessage(msg: Message) {
    when (msg.what) {
      MSG_REFRESH -> {
        "Group[${group.hexCode()}]_REFRESH_Begin".logDebug()
        val refreshTimeNano = measureNanoTime(group::performRefresh)
        "Group[${group.hexCode()}]_REFRESH_End".logDebug()
        "Group[${group.hexCode()}]_REFRESH [$=${refreshTimeNano / 1E6f}ms]".logInfo()
      }
      MSG_START_PLAYBACK -> (msg.obj as Playback).let(::startPlayback)
    }
  }

  private companion object {
    const val DEFAULT_DELAY_MS = 2 * 1000L / 60 // 2 frames in a 60fps system.
    const val MSG_REFRESH = 1
    const val MSG_START_PLAYBACK = 2
  }
}
