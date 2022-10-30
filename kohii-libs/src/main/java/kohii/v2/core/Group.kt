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

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.collection.arraySetOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v2.core.Playable.Command.PAUSED_BY_USER
import kohii.v2.internal.hexCode
import kohii.v2.internal.logDebug
import kohii.v2.internal.logInfo
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.system.measureNanoTime

/**
 * A [Group] represents an [Activity] in the Application. It manages [Manager]s and ensure the
 * [Playback]s are used correctly.
 */
internal class Group(
  @get:JvmSynthetic
  internal val home: Home,
  @get:JvmSynthetic
  internal val lifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver {

  @JvmSynthetic
  internal val managers = ArrayDeque<Manager>()
  private val dispatcher = GroupDispatcher(this, Looper.getMainLooper())

  override fun toString(): String {
    return "Group#${hexCode()}"
  }

  @JvmSynthetic
  internal fun addManager(manager: Manager) {
    if (managers.add(manager)) {
      onRefresh()
    }
  }

  @JvmSynthetic
  internal fun removeManager(manager: Manager) {
    if (managers.remove(manager)) {
      onRefresh()
    }
  }

  @JvmSynthetic
  internal fun onRefresh(): Unit = dispatcher.dispatchRefresh()

  @JvmSynthetic
  internal fun performRefresh() {
    val toPlay = linkedSetOf<Playback>() // Need the order.
    val toPause = arraySetOf<Playback>()

    managers.forEach { manager ->
      val (canPlay, canPause) = manager.splitPlaybacks()
      toPlay.addAll(canPlay)
      toPause.addAll(canPause)
    }

    toPause.forEach(dispatcher::dispatchPausePlayback)
    toPlay.forEach(dispatcher::dispatchStartPlayback)
    "Group#${hexCode()} refreshes".logDebug()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    owner.lifecycle.removeObserver(this)
    dispatcher.onDestroy()
    managers.clear()
    home.onGroupDestroyed(this)
  }

  private class GroupDispatcher(
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
          "$group refresh begins".logInfo()
          val refreshTimeNano = measureNanoTime(group::performRefresh)
          "$group refresh ends".logInfo()
          "$group refresh in ${NANOSECONDS.toMillis(refreshTimeNano)}ms".logDebug()
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
}
