package kohii.v2.core

import androidx.annotation.CallSuper
import kohii.v2.common.logInfo
import kohii.v2.common.logWarn
import kohii.v2.internal.asString
import kohii.v2.internal.hexCode

abstract class AbstractPlayable(
  home: Home,
  tag: String,
  data: Any,
  rendererType: Class<*>
) : Playable(home, tag, data, rendererType) {

  @CallSuper
  override fun onRelease() {
    "Playable[${hexCode()}]_RELEASE".logInfo()
  }

  @CallSuper
  override fun onRendererAttached(renderer: Any?) {
    "Playable[${hexCode()}]_ATTACH_Renderer [RR=${renderer?.asString()}]".logWarn()
  }

  @CallSuper
  override fun onRendererDetached(renderer: Any?) {
    "Playable[${hexCode()}]_DETACH_Renderer [RR=${renderer?.asString()}]".logWarn()
  }
}
