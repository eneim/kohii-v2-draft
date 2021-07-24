package kohii.v2.core

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v2.common.logDebug
import kohii.v2.internal.asString
import kohii.v2.internal.hexCode

class PlayerViewRendererProvider : RecycledRendererProvider() {

  override fun accepts(playable: Playable): Boolean {
    return playable.rendererType == PlayerView::class.java
  }

  override fun createRenderer(playback: Playback, rendererType: Int): Any {
    val container = playback.container
    require(container is ViewGroup)
    val result = LayoutInflater.from(container.context)
      .inflate(android.R.layout.simple_list_item_1, container, false) as TextView
    val params = result.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    )
    params.gravity = Gravity.CENTER
    result.gravity = Gravity.CENTER
    result.layoutParams = params

    "RendererProvider[${hexCode()}]_CREATE [RR=${result.asString()}]".logDebug()
    return result
  }
}
