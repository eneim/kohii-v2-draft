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

import android.graphics.Rect
import android.view.View
import androidx.annotation.FloatRange
import kohii.v2.core.Axis
import kohii.v2.core.Axis.HORIZONTAL
import kohii.v2.core.Axis.UNKNOWN
import kohii.v2.core.Axis.VERTICAL
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
  config: Config,
) : Playback(
  playable = playable,
  bucket = bucket,
  manager = manager,
  container = viewContainer,
  tag = tag,
  config = config,
) {

  private val tokenRect = Rect()
  private val drawRect = Rect()

  private var internalToken: ViewToken = ViewToken(-1f, tokenRect)

  override val token: Token get() = internalToken

  override val isOnline: Boolean
    get() = super.isOnline && viewContainer.isAttachedToWindow

  override fun shouldActivate(): Boolean = internalToken.activePixelsRatio > 0

  override fun shouldPrepare(): Boolean = internalToken.activePixelsRatio > 0

  override fun shouldPlay(): Boolean = internalToken.activePixelsRatio >= trigger

  override fun onRefresh() {
    val refreshTimeNano = measureNanoTime {
      super.onRefresh()
      internalToken = fetchToken()
    }
    "Playback[${hexCode()}]_REFRESH [$=${refreshTimeNano / 1E6f}ms]".logDebug()
  }

  private fun fetchToken(): ViewToken {
    checkMainThread()
    tokenRect.setEmpty()
    if (!isOnline) {
      return ViewToken(activePixelsRatio = -1f, boundInWindow = tokenRect)
    }
    if (!viewContainer.getGlobalVisibleRect(tokenRect)) {
      // tokenRect is updated, but the container is not visible in the Window, so reset it to empty.
      tokenRect.setEmpty()
      return ViewToken(activePixelsRatio = -1f, boundInWindow = tokenRect)
    }
    drawRect.setEmpty()
    val drawArea: Float = with(drawRect) drawRect@{
      viewContainer.getDrawingRect(this)
      viewContainer.clipBounds?.let(::intersect)
      this@drawRect.area.toFloat()
    }

    val activePixelsRatio: Float = drawArea.takeIf { it > 0 }
      ?.let { area -> tokenRect.area / area }
      ?: 0f
    return ViewToken(
      activePixelsRatio = activePixelsRatio,
      boundInWindow = tokenRect
    )
  }

  /**
   * @property activePixelsRatio Ratio of the pixels visible by the end user. -1 means it is too
   * faraway from the visible area.
   * @property boundInWindow The visible Rect of the Playback container in the current Window
   * dimension, or an empty Rect if the container has no pixel in the Window.
   */
  private class ViewToken(
    @FloatRange(from = -1.0, to = 1.0)
    val activePixelsRatio: Float,
    val boundInWindow: Rect,
  ) : Token() {

    override fun compare(other: Token, axis: Axis): Int {
      if (other !is ViewToken) return 1
      return when (axis) {
        VERTICAL -> compareValues(
          this.boundInWindow.exactCenterY(),
          other.boundInWindow.exactCenterY()
        )
        HORIZONTAL -> compareValues(
          this.boundInWindow.exactCenterX(),
          other.boundInWindow.exactCenterX()
        )
        UNKNOWN -> 0 // TODO: compare for both axis.
      }
    }
  }
}
