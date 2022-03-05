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
import kohii.v2.core.Home
import kohii.v2.core.Playable

internal class HomeDispatcher(private val home: Home) : Handler(Looper.getMainLooper()) {

  fun dispatchDestroyPlayable(
    playable: Playable,
    delayMillis: Long = DEFAULT_DELAY,
  ) = sendMessageDelayed(obtainMessage(MSG_DESTROY_PLAYABLE, playable), delayMillis)

  fun cancelPlayableDestroy(playable: Playable) = removeMessages(MSG_DESTROY_PLAYABLE, playable)

  override fun sendMessageAtTime(
    msg: Message,
    uptimeMillis: Long,
  ): Boolean {
    "Home[${home.hexCode()}]_SEND_Message [MS=${msg.hexCode()}, ${msg.what}]".logInfo()
    return super.sendMessageAtTime(msg, uptimeMillis)
  }

  override fun handleMessage(msg: Message) {
    "Home[${home.hexCode()}]_HANDLE_Message [MS=${msg.hexCode()}, ${msg.what}]".logDebug()
    when (msg.what) {
      MSG_DESTROY_PLAYABLE -> performDestroyPlayable(msg.obj as Playable)
    }
  }

  private fun performDestroyPlayable(playable: Playable) {
    "Home[${home.hexCode()}]_DESTROY_Playable [PB=$playable]".logInfo()
    playable.trySavePlayableState()
    playable.onPause()
    playable.onRelease()
    playable.onDestroy()
    home.playables.remove(playable)
  }

  private companion object {
    const val MSG_DESTROY_PLAYABLE = 1
    const val DEFAULT_DELAY = 0L
  }
}
