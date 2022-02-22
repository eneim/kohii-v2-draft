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

import android.view.View
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.PlayableKey.Data
import kohii.v2.core.PlayableKey.Empty
import kohii.v2.core.PlayableState.Initialized
import kohii.v2.core.Playback.Config
import kohii.v2.internal.BindRequest
import kotlin.LazyThreadSafetyMode.NONE

/**
 * See [Engine.setUp].
 */
class Binder(
  val request: Request,
  private val engine: Engine,
) {

  private val home = engine.manager.home

  /**
   * Copies the content of this [Binder] using an (optional) tag, returns a new [Binder] instance.
   */
  fun copy(tag: String? = null): Binder = Binder(
    request = request.copy(tag = tag),
    engine,
  )

  /**
   * Binds the request to a [container]. Returns a [RequestHandle] that can be used to wait for
   * the result of this request, or cancel it.
   *
   * @param container The container of the playback content. Currently only [View] is supported.
   * @throws IllegalArgumentException if the container is not of an supported type.
   */
  // TODO:
  //  - repeatMode API
  //  - resizeMode API
  @JvmOverloads
  fun bind(
    container: Any,
    config: Config.() -> Unit = { /* no-op */ },
  ): RequestHandle = bind(
    container = container,
    config = Config(binder = this).apply(config)
  )

  internal fun bind(
    container: Any,
    config: Config,
  ): RequestHandle {
    require(container is View /* || container is LifecycleOwner */) {
      "Currently, only View container is supported."
    }

    val bindRequest = BindRequest(
      home = home,
      manager = engine.manager,
      request = request,
      playableKey = request.tag?.let(::Data) ?: Empty,
      container = container,
      payload = preparePayload(),
      config = config
    )

    return home.enqueueRequest(container, bindRequest)
  }

  @OptIn(ExperimentalKohiiApi::class)
  private fun preparePayload(): Lazy<Playable> {
    val existingPlayable: Playable? = home.playables.entries
      .firstOrNull { (_, playableKey) -> playableKey !is Empty && playableKey.tag == request.tag }
      ?.key
    // The state that can be used to transfer to the new Playable for the same tag.
    val playableState: PlayableState? = existingPlayable?.currentState()

    if (existingPlayable != null) {
      require(existingPlayable.data == request.data) {
        "A playable tag ${request.tag} is used by different inputs data: " +
          "${request.data} and ${existingPlayable.data}"
      }

      if (existingPlayable.rendererType === engine.rendererType) {
        return lazyOf(existingPlayable) // Playable is reused.
      } else {
        // Same request is executed by a different Engine --> unbind the Playback.
        // `existingPlayable` will also be released.
        existingPlayable.playback?.unbind()
      }
    }

    return lazy(NONE) {
      val playableManager = engine.manager.playableManager
      val playable = engine.playableCreator.createPlayable(
        playableManager = playableManager,
        data = request.data,
        tag = request.tag ?: Home.NO_TAG,
      )

      playable.onCreate(
        initialState = playableState?.toBundle() // Transferred state.
          ?: playableManager.getPlayableState(playable) // PlayableManager managed state.
          ?: Initialized.toBundle() // Default initial state.
      )

      return@lazy playable
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Binder

    if (engine !== other.engine) return false
    if (request != other.request) return false

    return true
  }

  override fun hashCode(): Int {
    var result = request.hashCode()
    result = 31 * result + engine.hashCode()
    return result
  }
}
