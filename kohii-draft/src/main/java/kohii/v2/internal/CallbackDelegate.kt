package kohii.v2.internal

import kohii.v2.common.logInfo
import kohii.v2.core.Playable
import kohii.v2.core.Playback

/**
 * Used by [Playable] to register a [Playback.Callback] to the [Playback] it is bound to.
 */
internal class CallbackDelegate(val playable: Playable) : Playback.Callback {

  override fun onAdded(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_ADDED [PK=$playback]".logInfo()
  }

  override fun onRemoved(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_REMOVED [PK=$playback]".logInfo()
  }

  override fun onAttached(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_ATTACHED [PK=$playback]".logInfo()
    val manager = playable.manager ?: return
    manager.tryRestorePlayableState(playable)
  }

  override fun onDetached(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_DETACHED [PK=$playback]".logInfo()
    val manager = playable.manager ?: return
    manager.trySavePlayableState(playable)
  }

  override fun onActivated(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_ACTIVATED [PK=$playback]".logInfo()
  }

  override fun onDeactivated(playback: Playback) {
    "CallbackDelegate[${hexCode()}]_DEACTIVATED [PK=$playback]".logInfo()
  }
}
