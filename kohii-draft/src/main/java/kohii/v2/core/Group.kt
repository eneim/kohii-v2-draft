package kohii.v2.core

import android.os.Looper
import androidx.collection.arraySetOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v2.common.logDebug
import kohii.v2.internal.GroupDispatcher
import kohii.v2.internal.hexCode

class Group(
  val home: Home,
  val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

  internal val managers = ArrayDeque<Manager>()

  private val dispatcher = GroupDispatcher(this, Looper.getMainLooper())
  private val playbacks: Collection<Playback>
    get() = managers.flatMap { manager -> manager.playbacks.values }

  override fun toString(): String = "G@${hexCode()}"

  internal fun addManager(manager: Manager) {
    managers.add(manager)
    onRefresh()
  }

  internal fun removeManager(manager: Manager) {
    managers.remove(manager)
    onRefresh()
  }

  internal fun onRefresh(): Unit = dispatcher.dispatchRefresh()

  internal fun performRefresh() {
    "Group[${hexCode()}]_REFRESH_Begin".logDebug()
    val playbacks = this.playbacks
    playbacks.forEach(Playback::onRefresh)

    val toPlay = linkedSetOf<Playback>() // Need the order.
    val toPause = arraySetOf<Playback>()

    managers.forEach { manager ->
      val (canPlay, canPause) = manager.splitPlaybacks()
      toPlay.addAll(canPlay)
      toPause.addAll(canPause)
    }

    toPause.forEach(dispatcher::dispatchPausePlayback)
    toPlay.forEach(dispatcher::dispatchStartPlayback)
    "Group[${hexCode()}]_REFRESH_End".logDebug()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    owner.lifecycle.removeObserver(this)
    dispatcher.onDestroy()
    managers.clear()
    home.onGroupDestroyed(this)
  }
}
