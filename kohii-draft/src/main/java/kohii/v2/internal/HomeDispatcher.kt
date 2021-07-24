package kohii.v2.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import kohii.v2.common.logInfo
import kohii.v2.core.Home
import kohii.v2.core.Playable

internal class HomeDispatcher(
  private val home: Home,
  looper: Looper
) : Handler(looper) {

  fun dispatchReleasePlayable(
    playable: Playable
  ) = obtainMessage(MSG_RELEASE_PLAYABLE, playable).sendToTarget()

  fun dispatchDestroyPlayable(
    playable: Playable
  ) = obtainMessage(MSG_DESTROY_PLAYABLE, playable).sendToTarget()

  fun cancelPlayableRelease(playable: Playable) = removeMessages(MSG_RELEASE_PLAYABLE, playable)

  fun cancelPlayableDestroy(playable: Playable) = removeMessages(MSG_DESTROY_PLAYABLE, playable)

  override fun handleMessage(msg: Message) {
    when (msg.what) {
      MSG_RELEASE_PLAYABLE -> performReleasePlayable(msg.obj as Playable)
      MSG_DESTROY_PLAYABLE -> performDestroyPlayable(msg.obj as Playable)
    }
  }

  private fun performReleasePlayable(playable: Playable) {
    "Home[${Integer.toHexString(home.hashCode())}]_RELEASE_Playable [PB=$playable]".logInfo()
    playable.onPause()
    playable.onRelease()
  }

  private fun performDestroyPlayable(playable: Playable) {
    "Home[${Integer.toHexString(home.hashCode())}]_DESTROY_Playable [PB=$playable]".logInfo()
    home.playables.remove(playable)
  }

  private companion object {
    const val MSG_RELEASE_PLAYABLE = 1
    const val MSG_DESTROY_PLAYABLE = 2
  }
}
