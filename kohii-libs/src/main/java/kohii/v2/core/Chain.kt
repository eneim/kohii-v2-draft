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

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.source.ShuffleOrder.UnshuffledShuffleOrder
import kohii.v2.common.ExperimentalKohiiApi
import kohii.v2.core.Chain.SelectScope.ALL
import kohii.v2.core.Chain.SelectScope.AVAILABLE_ONLY
import kohii.v2.internal.hexCode
import kohii.v2.internal.logInfo
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Combines many [Playback]s into a chain.
 *
 * A [Chain] is an abstract group that can contains multiple [Playback]s. The [Playback]s are
 * started in a specific order defined by the chain.
 *
 * If one of the [Playback]s are started by user, it always gets the highest playback priority. For
 * example if a chain of [Playback]s A, B, C supposes to play in that order, but if B is started
 * manually, it will always be selected for starting. To reset this state, either manually start
 * all other [Playback]s, or clear the started state of the [Playback] B in the example using the
 * [Playback.controller].
 *
 * A [Chain] can be constructed using the [Bucket.chain] method.
 */
@OptIn(UnstableApi::class)
@ExperimentalKohiiApi
class Chain private constructor(
  private val bucket: Bucket,
  private val tags: List<String>,
  private val loop: Boolean = true,
  private val selectScope: SelectScope,
) : PlayerEventListener {

  private val playbacks = mutableMapOf<String, Playback?>()
  private val playbackOrder: ShuffleOrderWrapper = ShuffleOrderWrapper(
    origin = UnshuffledShuffleOrder(tags.size),
    loop = loop
  )

  private val targetIndex = AtomicInteger(playbackOrder.firstIndex)

  init {
    for (tag in tags) {
      bucket.scope.launch {
        bucket.manager.getPlaybackFlow(tag).collect { playback: Playback? ->
          playbacks[tag] = playback
          if (playback != null) {
            playback.chain = this@Chain
            playback.addPlayerEventListener(this@Chain)
          }
        }
      }
    }
  }

  // TODO: take into account the manual commands.
  internal fun selectToPlay(candidates: Collection<Playback>): Collection<Playback> {
    return when (selectScope) {
      ALL -> {
        tags
          .filterIndexed { index, _ -> index == targetIndex.get() }
          .mapNotNull { tag -> playbacks[tag] }
          .intersect(candidates)
      }
      AVAILABLE_ONLY -> {
        listOfNotNull(
          playbackOrder.indices.entries
            .asSequence()
            // Drop those are of out-of-scope indices.
            .dropWhile { (index, _) -> index != targetIndex.get() }
            .map { entry -> playbacks[tags[entry.key]] }
            // Drop those are not ready to play immediately.
            .dropWhile { playback -> playback == null || !playback.shouldPlay() }
            .firstOrNull()
        )
          .intersect(candidates)
      }
    }
  }

  override fun onStateChanged(
    playback: Playback,
    state: Int,
  ) {
    "Chain[${hexCode()}]_StateChanged: $state, pk: $playback".logInfo()
    if (state == Player.STATE_ENDED) {
      if (loop) {
        playback.playable.onReset()
      }
      val currentIndex = tags.indexOfFirst { tag -> tag == playback.tag }
      if (currentIndex >= 0) {
        val nextIndex = playbackOrder.getNextIndex(currentIndex)
        targetIndex.set(nextIndex)
        bucket.manager.refresh()
      }
    }
  }

  class Builder(
    private val bucket: Bucket,
    private val loop: Boolean,
    private val selectScope: SelectScope,
  ) {

    private val tags = linkedMapOf<String, Unit>()

    fun addPlaybackTag(tag: String) = apply {
      require(tag.isNotBlank()) { "Tag must not blank." }
      tags.remove(tag)
      tags[tag] = Unit
    }

    internal fun build(): Chain = Chain(
      bucket = bucket,
      tags = tags.keys.toList(),
      loop = loop,
      selectScope = selectScope,
    )
  }

  enum class SelectScope {
    ALL,
    AVAILABLE_ONLY,
  }

  private class ShuffleOrderWrapper(
    private val origin: ShuffleOrder,
    private val loop: Boolean = false,
  ) : ShuffleOrder by origin {

    val indices: Map<Int, Int>

    init {
      val indexCache = mutableMapOf<Int, Int>()
      var index = origin.firstIndex
      while (true) {
        val nextIndex = origin.getNextIndex(index)
        indexCache[index] = nextIndex
        index = nextIndex
        if (index == C.INDEX_UNSET) break
      }
      this.indices = indexCache
    }

    override fun getFirstIndex(): Int {
      return if (length > 0) indices.keys.first() else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int {
      return if (length > 0) indices.keys.last() else C.INDEX_UNSET
    }

    override fun getNextIndex(index: Int): Int =
      if (loop && index == origin.lastIndex) {
        origin.firstIndex
      } else {
        indices.getOrElse(index) { C.INDEX_UNSET }
      }

    override fun getPreviousIndex(index: Int): Int =
      if (loop && index == origin.firstIndex) {
        origin.lastIndex
      } else {
        indices.getOrElse(index) { C.INDEX_UNSET }
      }
  }
}
