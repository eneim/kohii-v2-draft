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

import kohii.v2.core.Playable.Command
import kohii.v2.core.Playable.Command.PAUSED_BY_USER
import kohii.v2.core.Playable.Command.STARTED_BY_USER
import kohii.v2.core.Playable.Controller
import kohii.v2.core.Playback

internal class PlayableControllerImpl(
  private val playback: Playback,
) : Controller {

  override fun play() = setPlayableCommand(STARTED_BY_USER)

  override fun pause() = setPlayableCommand(PAUSED_BY_USER)

  override fun auto() = setPlayableCommand(null)

  private fun setPlayableCommand(value: Command?) {
    check(playback.isAdded) { "Playback $playback is not available." }
    val current = playback.playable.command.get()
    if (current == value) return

    // TODO: consider different scenarios, such as a manual START after a manual PAUSE.
    playback.playable.command.set(value)
    playback.manager.refresh()
  }
}
