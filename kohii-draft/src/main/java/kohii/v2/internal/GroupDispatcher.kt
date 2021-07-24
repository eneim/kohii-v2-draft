package kohii.v2.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import kohii.v2.common.logInfo
import kohii.v2.core.Group
import kohii.v2.core.Playback
import kotlin.system.measureNanoTime

internal class GroupDispatcher(
  private val group: Group,
  looper: Looper
) : Handler(looper) {

  fun dispatchRefresh() {
    removeMessages(MSG_REFRESH)
    sendEmptyMessageDelayed(MSG_REFRESH, DEFAULT_DELAY_MS)
  }

  fun dispatchStartPlayback(playback: Playback, delay: Long = 0) {
    removeMessages(MSG_START_PLAYBACK, playback)
    if (delay <= 0) {
      obtainMessage(MSG_START_PLAYBACK, playback).sendToTarget()
    } else {
      sendMessageDelayed(
        obtainMessage(MSG_START_PLAYBACK, playback),
        delay
      )
    }
  }

  fun dispatchPausePlayback(playback: Playback) {
    removeMessages(MSG_START_PLAYBACK, playback)
    pausePlayback(playback)
  }

  fun onDestroy(): Unit = removeCallbacksAndMessages(null)

  private fun startPlayback(playback: Playback) {
    group.home.startPlayable(playback.playable)
  }

  private fun pausePlayback(playback: Playback) {
    group.home.pausePlayable(playback.playable)
  }

  override fun handleMessage(msg: Message) {
    when (msg.what) {
      MSG_REFRESH -> {
        val refreshTimeNano = measureNanoTime(group::performRefresh)
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
