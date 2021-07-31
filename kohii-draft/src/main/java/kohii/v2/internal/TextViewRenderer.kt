package kohii.v2.internal

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import kohii.v2.common.logDebug
import kohii.v2.core.AbstractPlayable
import kohii.v2.core.Home
import kohii.v2.core.Playable
import kohii.v2.core.PlayableCreator
import kohii.v2.core.PlayableManager
import kohii.v2.core.PlayableState
import kohii.v2.core.PlayableState.DebugPlayableState
import kohii.v2.core.PlayableState.Unknown
import kohii.v2.core.Playback
import kohii.v2.core.RecycledRendererProvider

private class TextViewPlayable(home: Home, tag: String, data: Any) :
  AbstractPlayable(home, tag, data, TextView::class.java) {

  override val renderer: TextView? get() = internalRenderer
  override val isPlaying: Boolean get() = renderer?.text?.startsWith("START") == true

  private var internalRenderer: TextView? = null

  override fun fetchPlayableState(): PlayableState {
    val playback = internalPlayback as? ViewPlayback ?: return Unknown
    return DebugPlayableState(
      playback.token,
      renderer
    )
  }

  override fun onRendererAttached(renderer: Any?) {
    super.onRendererAttached(renderer)
    internalRenderer = renderer as? TextView
  }

  override fun onRendererDetached(renderer: Any?) {
    super.onRendererDetached(renderer)
    if (internalRenderer === renderer) {
      internalRenderer = null
    }
  }

  @SuppressLint("SetTextI18n")
  override fun onStart() {
    super.onStart()
    internalRenderer?.text = "STARTED [$tag]"
  }

  @SuppressLint("SetTextI18n")
  override fun onPause() {
    internalRenderer?.text = "PAUSED [$tag]"
    super.onPause()
  }

  override fun onRelease() {
    super.onRelease()
    internalRenderer?.text = ""
  }
}

internal class TextViewPlayableCreator(val home: Home) : PlayableCreator {

  override fun accepts(mediaData: Any): Boolean = true

  override fun createPlayable(playableManager: PlayableManager, data: Any, tag: String): Playable {
    return TextViewPlayable(home, tag, data)
  }

  override fun cleanUp(): Unit = Unit
}

internal class TextViewRendererProvider(poolSize: Int = 2) : RecycledRendererProvider(poolSize) {

  override fun accepts(playback: Playback): Boolean {
    return playback.container is ViewGroup &&
        TextView::class.java.isAssignableFrom(playback.playable.rendererType)
  }

  override fun createRenderer(playback: Playback, rendererType: Int): TextView {
    val container = playback.container as ViewGroup
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

  override fun recycleRenderer(renderer: Any?) {
    "RendererProvider[${hexCode()}]_RECYCLE [RR=${renderer?.asString()}]".logDebug()
    (renderer as? TextView)?.text = ""
  }
}
