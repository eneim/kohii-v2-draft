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
import android.os.Looper
import androidx.collection.arraySetOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v2.internal.GroupDispatcher
import kohii.v2.internal.hexCode

/**
 * A [Group] represents an [Activity] in the Application. It manages [Manager]s and ensure the
 * [Playback]s are used correctly.
 */
class Group(
  val home: Home,
  val lifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver {

  internal val managers = ArrayDeque<Manager>()
  private val dispatcher = GroupDispatcher(this, Looper.getMainLooper())

  override fun toString(): String = "G@${hexCode()}"

  internal fun addManager(manager: Manager) {
    if (managers.add(manager)) {
      onRefresh()
    }
  }

  internal fun removeManager(manager: Manager) {
    if (managers.remove(manager)) {
      onRefresh()
    }
  }

  internal fun onRefresh(): Unit = dispatcher.dispatchRefresh()

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
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    owner.lifecycle.removeObserver(this)
    dispatcher.onDestroy()
    managers.clear()
    home.onGroupDestroyed(this)
  }
}
