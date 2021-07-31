package kohii.v2.core

import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.Timeline
import kohii.v2.core.Playback.Token

/**
 * Represents an immediate state of a [Playable].
 */
sealed class PlayableState {

  object Unknown : PlayableState()

  // A state where the Playable is not active in any Playback.
  object Inactive : PlayableState()

  object Stopped : PlayableState()

  object Released : PlayableState()

  data class PlaybackLatestState(
    val totalDurationMillis: Long, // Ads + Content
    val contentDurationMillis: Long, // Content only
    val positionMillis: Long,
    val timeline: Timeline,
    val windowIndex: Int,
    val periodIndex: Int,
    val metaData: MediaMetadata,
    val playing: Boolean,
  ) : PlayableState()

  internal data class DebugPlayableState(
    val token: Token,
    val renderer: Any?,
  ) : PlayableState()
}
