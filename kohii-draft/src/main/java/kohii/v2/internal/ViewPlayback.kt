package kohii.v2.internal

import android.graphics.Rect
import android.view.View
import androidx.annotation.FloatRange
import kohii.v2.common.logDebug
import kohii.v2.core.Bucket
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.Playback
import kotlin.system.measureNanoTime

/**
 * An abstract [Playback] that has a [View] container.
 */
internal abstract class ViewPlayback(
  playable: Playable,
  bucket: Bucket,
  manager: Manager,
  val viewContainer: View,
  tag: String,
) : Playback(
  playable = playable,
  bucket = bucket,
  manager = manager,
  container = viewContainer,
  tag = tag
) {

  private val tokenRect = Rect()
  private val drawRect = Rect()

  private var internalToken: ViewToken = ViewToken(-1f)

  override val token: Token get() = internalToken

  override val isOnline: Boolean
    get() = super.isOnline && viewContainer.isAttachedToWindow

  override fun shouldActivate(): Boolean = internalToken.activePixelsRatio > 0

  override fun shouldPrepare(): Boolean = internalToken.activePixelsRatio > 0

  override fun shouldPlay(): Boolean = internalToken.activePixelsRatio >= 0.65

  override fun onRefresh() {
    val refreshTimeNano = measureNanoTime {
      super.onRefresh()
      internalToken = fetchToken()
    }
    "Playback[${hexCode()}]_REFRESH [$=${refreshTimeNano / 1E6f}ms]".logDebug()
  }

  private fun fetchToken(): ViewToken {
    checkMainThread()
    if (!isOnline) {
      return ViewToken(-1f)
    }
    tokenRect.setEmpty()
    if (!viewContainer.getGlobalVisibleRect(tokenRect)) {
      return ViewToken(-1f)
    }
    drawRect.setEmpty()
    val drawArea: Float = with(drawRect) {
      viewContainer.getDrawingRect(this)
      viewContainer.clipBounds?.let(::intersect)
      area.toFloat()
    }

    val activePixelsRatio: Float = drawArea.takeIf { it > 0 }
      ?.let { area -> tokenRect.area / area }
      ?: 0f
    return ViewToken(activePixelsRatio = activePixelsRatio)
  }

  /**
   * @property activePixelsRatio Ratio of the pixels visible by the end user. -1 means it is too
   * faraway from the visible area.
   */
  private data class ViewToken(
    @FloatRange(from = -1.0, to = 1.0)
    val activePixelsRatio: Float,
  ) : Token()
}
