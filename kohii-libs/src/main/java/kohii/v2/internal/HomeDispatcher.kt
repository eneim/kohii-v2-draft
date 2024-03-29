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

private const val DEFAULT_DELAY = 0L
private const val MSG_DESTROY_PLAYABLE = 1

internal class HomeDispatcher(private val home: Home) : Handler(Looper.getMainLooper()) {

  fun dispatchDestroyPlayable(
    playable: Playable,
    delayMillis: Long = DEFAULT_DELAY,
  ) {
    "Will destroy: $playable".logStackTrace()
    sendMessageDelayed(
      /* msg = */ obtainMessage(/* what = */ MSG_DESTROY_PLAYABLE, /* obj = */ playable),
      /* delayMillis = */ delayMillis
    )
  }

  fun cancelPlayableDestroy(playable: Playable) {
    "Stop destroy: $playable".logStackTrace()
    removeMessages(MSG_DESTROY_PLAYABLE, playable)
  }

  override fun handleMessage(msg: Message) {
    when (msg.what) {
      MSG_DESTROY_PLAYABLE -> performDestroyPlayable(msg.obj as Playable)
    }
  }

  private fun performDestroyPlayable(playable: Playable) {
    playable.trySavePlayableState()
    playable.onPause()
    playable.onRelease()
    playable.onDestroy()
    home.playables.remove(playable)
    "$playable is destroyed.".logInfo()
  }
}
