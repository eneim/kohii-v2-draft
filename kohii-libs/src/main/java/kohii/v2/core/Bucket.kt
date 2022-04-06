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
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Chain.SelectScope
import kohii.v2.core.Chain.SelectScope.ALL
import kohii.v2.core.Playable.Command.STARTED_BY_USER
import kohii.v2.internal.asString
import kohii.v2.internal.checkMainThread
import kohii.v2.internal.hexCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * A class that manages a collection of container and takes care of their lifecycles. A bucket is
 * always managed by a [Manager].
 *
 * @property manager The [Manager] that manages this [Bucket].
 * @property root The root of this [Bucket]. If a [Bucket] is created for a [RecyclerView], that
 * [RecyclerView] is the root of that [Bucket].
 */
abstract class Bucket(
  val manager: Manager,
  val root: Any,
  private val selector: Selector = defaultSelector,
) {

  internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override fun toString(): String = "B[${hexCode()}, rt=${root.asString()}]"

  /**
   * Called when this class is added to a [Manager].
   */
  @CallSuper
  @MainThread
  internal open fun onAdd(): Unit = checkMainThread()

  /**
   * Called when this class is removed from a [Manager].
   */
  @CallSuper
  @MainThread
  internal open fun onRemove() {
    checkMainThread()
    scope.cancel()
  }

  /**
   * Called when the [root] becomes available. For example if [root] is a [View], it is when the
   * root is attached to the Window.
   */
  @CallSuper
  @MainThread
  internal open fun onStart(): Unit = checkMainThread()

  /**
   * Called when the [root] becomes unavailable. For example if [root] is a [View], it is when the
   * root is detached to the Window.
   */
  @CallSuper
  @MainThread
  internal open fun onStop(): Unit = checkMainThread()

  /**
   * Overwrite to perform adding a container to this Bucket.
   */
  @CallSuper
  @MainThread
  internal open fun addContainer(container: Any) = checkMainThread()

  /**
   * Overwrite to perform removing a container to this Bucket.
   */
  @CallSuper
  @MainThread
  internal open fun removeContainer(container: Any) = checkMainThread()

  /**
   * Returns `true` if this Bucket accepts the [container], `false` otherwise.
   */
  @MainThread
  internal open fun accept(container: Any): Boolean = false

  /**
   * Returns `true` if the [playback] meets the conditions to start playing.
   */
  @MainThread
  internal open fun allowToPlay(playback: Playback): Boolean {
    checkMainThread()
    return playback.shouldPlay()
  }

  /**
   * This method must returns all [Playback]s that can start playing, regardless of the Bucket's
   * policy and their manual state. [selectToPlay] then applies the correct adjustment to output
   * the finalist.
   */
  @MainThread
  protected open fun selectToPlayInternal(candidates: Collection<Playback>): Collection<Playback> {
    return emptyList()
  }

  /**
   * From the collection of candidate [Playback]s, returns a collection of [Playback]s that will be
   * used to start playing. By default, this method returns an empty list.
   */
  @OptIn(ExperimentalKohiiApi::class)
  @MainThread
  internal fun selectToPlay(candidates: Collection<Playback>): Collection<Playback> {
    checkMainThread()
    val finalCandidates = candidates
      .filter { playback -> playback.playable.command.get() == STARTED_BY_USER }
      .takeIf(List<Playback>::isNotEmpty)
      ?: candidates

    val selected = selectToPlayInternal(finalCandidates)

    return selected.groupBy { it.chain ?: Unit }
      .flatMap { (key, playbacks) ->
        if (key !is Chain) playbacks
        else key.selectToPlay(finalCandidates)
      }
      .let(selector::select)
  }

  //region Public APIs
  /**
   * Starts a [Chain] using the [Chain.Builder].
   *
   * Unlike normal [Engine.setUp] or methods, all [Playback]s in a [Chain] must be bound to
   * containers of the same [Bucket]. Therefore this method belongs to the Bucket.
   */
  @ExperimentalKohiiApi
  @JvmOverloads
  fun chain(
    loop: Boolean = false,
    selectScope: SelectScope = ALL,
    builder: Chain.Builder.() -> Unit,
  ) = Chain.Builder(
    bucket = this,
    loop = loop,
    selectScope = selectScope
  )
    .apply(builder)
    .build()
  //endregion

  interface Selector {

    fun select(candidates: Collection<Playback>): Collection<Playback>
  }

  companion object {

    private val defaultSelector: Selector = object : Selector {
      override fun select(candidates: Collection<Playback>): Collection<Playback> {
        return listOfNotNull(candidates.firstOrNull())
      }
    }

    internal operator fun get(
      manager: Manager,
      root: Any,
    ): Bucket = when (root) {
      is ViewGroup -> ViewBucket[manager, root] // Delegate the creation to ViewBucket.
      else -> throw IllegalArgumentException("$root is not supported yet.")
    }
  }
}
