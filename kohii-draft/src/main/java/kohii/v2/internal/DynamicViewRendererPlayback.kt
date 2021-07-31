/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

import android.view.View
import android.view.ViewGroup
import androidx.core.view.contains
import kohii.v2.common.logDebug
import kohii.v2.core.Bucket
import kohii.v2.core.Manager
import kohii.v2.core.Playable
import kohii.v2.core.RendererProviderManager
import kohii.v2.internal.RendererResult.RENDERER_IS_NULL
import kohii.v2.internal.RendererResult.RENDERER_WAS_ATTACHED
import kohii.v2.internal.RendererResult.RENDERER_WAS_DETACHED

/**
 * A [ViewPlayback] where its [ViewPlayback.container] is a [ViewGroup] that can contain a [View]
 * which is the actual renderer for the playback. This renderer can be added/removed dynamically.
 */
internal class DynamicViewRendererPlayback(
  playable: Playable,
  bucket: Bucket,
  manager: Manager,
  container: ViewGroup,
  providerManager: RendererProviderManager,
  tag: String = "",
) : ViewPlayback(
  playable = playable,
  bucket = bucket,
  manager = manager,
  viewContainer = container,
  tag = tag
) {

  private val rendererProvider = providerManager.getRendererProvider(this)

  override fun onStarted() {
    super.onStarted()
    tryAttachRenderer()
  }

  override fun onPaused() {
    tryDetachRenderer()
    super.onPaused()
  }

  private fun tryAttachRenderer() {
    if (playable.renderer == null) {
      val renderer = rendererProvider.provideRenderer(this)
      val attachRendererInfo = playbackAttachRenderer(renderer)
      if (attachRendererInfo == null) {
        playable.onRendererAttached(renderer)
      } else {
        "Playback[${hexCode()}]_ATTACH_Renderer [RR=${renderer?.asString()}] fails, result=$attachRendererInfo".logDebug()
      }
    }
  }

  private fun tryDetachRenderer() {
    val renderer = playable.renderer
    if (renderer != null) {
      val detachRenderInfo = playbackDetachRenderer(renderer)
      if (detachRenderInfo == null) {
        playable.onRendererDetached(renderer)
        rendererProvider.releaseRenderer(
          playback = this,
          renderer = renderer
        )
      } else {
        "Playback[${hexCode()}]_DETACH_Renderer [RR=${renderer.asString()}] fails, result=$detachRenderInfo".logDebug()
      }
    }
  }

  /**
   * Returns a [RendererResult] if it false to attach, or the renderer is already attached.
   * Null otherwise.
   */
  private fun playbackAttachRenderer(renderer: Any?): RendererResult? {
    if (renderer == null) return RENDERER_IS_NULL
    require(renderer is View && container is ViewGroup && renderer !== container)
    if (container.contains(renderer)) return RENDERER_WAS_ATTACHED

    val parent = renderer.parent
    if (parent is ViewGroup && parent !== container) {
      parent.removeView(renderer)
    }

    container.removeAllViews()
    container.addView(renderer)
    return null
  }

  /**
   * Returns a [RendererResult] if it false to detach, or the renderer is already detached.
   * Null otherwise.
   */
  private fun playbackDetachRenderer(renderer: Any?): RendererResult? {
    if (renderer == null) return RENDERER_IS_NULL
    require(renderer is View && container is ViewGroup && renderer !== container)
    if (!container.contains(renderer)) return RENDERER_WAS_DETACHED
    container.removeView(renderer)
    return null
  }

  override fun detachRenderer(): Unit = tryDetachRenderer()
}
