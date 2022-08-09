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

import android.app.Application
import android.os.Bundle
import android.util.Range
import androidx.annotation.CallSuper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import kohii.v2.core.Home.Companion.NO_TAG
import kohii.v2.core.Manager.Companion.DEFAULT_DESTRUCTION_DELAY_MS
import kohii.v2.core.Playback.Config
import kohii.v2.internal.debugOnly
import kohii.v2.internal.hexCode
import kohii.v2.internal.logStackTrace
import kohii.v2.internal.playbackLifecycleCallback
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

abstract class Playable(
  val home: Home,
  val tag: String,
  val data: List<RequestData>,
  val rendererType: Class<*>,
  initialManager: PlayableManager,
) {

  internal val internalId = UUID.randomUUID().toString()

  abstract val isStarted: Boolean

  abstract val isPlaying: Boolean

  abstract val renderer: Any?

  abstract val triggerRange: Range<Float>

  /**
   * The current [PlayableManager].
   */
  private var manager: PlayableManager = initialManager
    set(value) {
      val prev = field
      field = value
      if (prev !== value) {
        val state = prev.removePlayable(playable = this, clearState = true)
        value.addPlayable(playable = this, state = state)
        onManagerChanged(previous = prev, next = value)
      }
    }

  /**
   * The current [Playback] that is bound to this [Playable].
   */
  var playback: Playback? = null
    @JvmSynthetic internal set(value) {
      val prev = field
      field = value
      if (value != null) manager = value.manager.playableManager
      if (prev !== value) onPlaybackChanged(previous = prev, next = value)
      if (value == null) {
        val destroyPlayableDelay = if (prev != null && prev.manager.isChangingConfigurations) {
          DEFAULT_DESTRUCTION_DELAY_MS
        } else 0
        home.destroyPlayableDelayed(playable = this, delayMillis = destroyPlayableDelay)
      }
    }

  //region Manual control support
  // TODO: save and restore this value across recreation.
  internal val command = AtomicReference<Command>(null)
  //endregion

  private val lifecycleCallback: LifecycleCallback by playbackLifecycleCallback()

  init {
    "Playable[${hexCode()}] is created.".logStackTrace()
  }

  /**
   * Called when a new [Playable] instance is created.
   *
   * @param initialState A [Bundle] that can be used to initialize the [Playable]. This state can be
   * from a another [Playable] that plays the same data but uses a different rendering
   * implementation, or a state previously managed by the [PlayableManager] (e.g. a [Playable] was
   * destroyed due to its Playback being destroyed, but its state is stored and now it needs to be
   * back to that state), or a default initial state ([PlayableState.Initialized]).
   */
  @CallSuper
  open fun onCreate(initialState: Bundle) {
    manager.addPlayable(this, initialState)
  }

  /**
   * Notify the Playable that it is destroyed. This method is called right before the
   * destruction of the [Playable] instance. The [Playable] instance is no longer used again after
   * this method is called.
   */
  @CallSuper
  open fun onDestroy() {
    // On normal destruction, we do not need to clear the state. When the manager is cleared, it
    // will clear them automatically.
    manager.removePlayable(playable = this, clearState = false)
  }

  /**
   * Returns the [Bundle] that contains the state of this [Playable]. This value will be used to
   * restore the [Playable] later.
   *
   * See [onRestoreState].
   */
  open fun onSaveState(): Bundle = Bundle.EMPTY

  /**
   * Restores this [Playable] to a previous state using the value from [state].
   *
   * See [onSaveState].
   */
  open fun onRestoreState(state: Bundle) = Unit

  /**
   * Called by the [Playback] to attach a renderer to this [Playable].
   */
  abstract fun onRendererAttached(renderer: Any?)

  /**
   * Called by the [Playback] to detach the renderer from this [Playable].
   */
  abstract fun onRendererDetached(renderer: Any?)

  /**
   * Returns the immediate state of this [Playable].
   */
  abstract fun currentState(): PlayableState

  @CallSuper
  open fun onPrepare(preload: Boolean) = Unit

  @CallSuper
  open fun onReady() = Unit

  @CallSuper
  open fun onStart() {
    playback?.onStarted()
  }

  @CallSuper
  open fun onPause() {
    playback?.onPaused()
  }

  /**
   * Reset the state of this [Playable] so that it can start from the beginning. Another call to
   * [onPrepare] is required to restart the playback.
   */
  open fun onReset() = Unit

  /**
   * Releases the resource hold by this [Playable]. This method is called when the [Playable] is
   * destroyed, or it becomes inactive so that it needs to give others the resources for playback.
   */
  abstract fun onRelease()

  /**
   * Called when the [Playback] bound to this [Playable] is changed.
   *
   * It can be when a [Playable] is bound to a [Playback] for the first time (in which case
   * [previous] is null and [next] is not null), or is switched from a [Playback] to another
   * [Playback] (in which case both [previous] and [next] are not null), or is unbound from a
   * [Playback] (in which case [previous] is not null and [next] is null). Note that there should
   * not be the case both [previous] and [next] are both null.
   */
  @CallSuper
  protected open fun onPlaybackChanged(
    previous: Playback?,
    next: Playback?,
  ) {
    previous?.removeLifecycleCallback(lifecycleCallback)
    next?.addLifecycleCallback(lifecycleCallback)
  }

  @CallSuper
  protected open fun onManagerChanged(
    previous: PlayableManager,
    next: PlayableManager,
  ) {
    "Playable[${hexCode()}]_CHANGE_Manager [$previous → $next]".logStackTrace()
  }

  @JvmSynthetic
  internal fun trySavePlayableState() {
    manager.trySavePlayableState(this)
  }

  @JvmSynthetic
  internal fun tryRestorePlayableState() {
    manager.tryRestorePlayableState(this)
  }

  /**
   * Supported commands sent from the client.
   */
  enum class Command {
    STARTED_BY_USER,
    PAUSED_BY_USER,
  }

  interface Controller {

    /**
     * Starts a playback as long as the container reaches the [Config.trigger] value.
     */
    fun play() = Unit

    /**
     * Pauses the [Playback]. Until another call to [play], or the underneath [Playable] is rebound
     * to another container, it will not start again.
     */
    fun pause() = Unit

    /**
     * Clears any [Command] set to the [Playable].
     */
    fun auto() = Unit

    companion object : Controller
  }

  internal class ManagerImpl(
    application: Application,
    private val stateHandle: SavedStateHandle,
  ) : AndroidViewModel(application), PlayableManager {

    private val home: Home = Home[application]

    /**
     * A [Set] of [Playable]s managed by this class.
     */
    private val playables = mutableSetOf<Playable>()

    override fun addPlayable(
      playable: Playable,
      state: Bundle?,
    ) {
      if (playables.add(playable)) {
        if (state != null) {
          stateHandle[playable.stateKey] = state
        }
      }
    }

    override fun removePlayable(
      playable: Playable,
      clearState: Boolean,
    ): Bundle? {
      return if (playables.remove(playable)) {
        if (clearState) stateHandle.remove<Bundle>(playable.stateKey) else null
      } else {
        null
      }
    }

    override fun getPlayableState(playable: Playable): Bundle? {
      return stateHandle.get(playable.stateKey)
    }

    override fun trySavePlayableState(playable: Playable) {
      if (stateHandle.contains(playable.stateKey)) return
      val playableState = playable.onSaveState()
      if (playableState != Bundle.EMPTY) {
        stateHandle[playable.stateKey] = playableState
      }
    }

    override fun tryRestorePlayableState(playable: Playable) {
      val savedState: Bundle? = stateHandle.remove<Bundle>(playable.stateKey)
      savedState?.let(playable::onRestoreState)
    }

    override fun onCleared() {
      super.onCleared()
      if (playables.isNotEmpty()) {
        playables.toMutableSet()
          .onEach { playable ->
            debugOnly { check(playable.manager === this) }
            home.destroyPlayableDelayed(playable = playable, delayMillis = 0)
          }
          .clear()
      }
    }
  }
}

internal sealed class PlayableKey(val tag: String) {

  object Empty : PlayableKey(NO_TAG)

  class Data(tag: String) : PlayableKey(tag = tag)
}

private val Playable.stateKey: String get() = tag.takeIf(String::isNotEmpty) ?: internalId
