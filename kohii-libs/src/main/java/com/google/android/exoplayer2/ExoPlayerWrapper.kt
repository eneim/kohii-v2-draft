/*
 * Copyright (c) 2022. Nam Nguyen, nam@ene.im
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

package com.google.android.exoplayer2

import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import kohii.v2.exoplayer.StyledPlayerViewBridge

/**
 * A wrapper for [ExoPlayer] that also exposes the [DefaultMediaSourceFactory] instance if it is use
 * to create the player instance. This is used by the [StyledPlayerViewBridge] to setup for Ad
 * playback.
 */
internal class ExoPlayerWrapper private constructor(
  private val player: ExoPlayer,
  val mediaSourceFactory: DefaultMediaSourceFactory?,
) : ExoPlayer by player {

  private constructor(
    builder: ExoPlayer.Builder,
    mediaSourceFactory: MediaSourceFactory
  ) : this(
    player = builder
      .setMediaSourceFactory(mediaSourceFactory)
      .build(),
    mediaSourceFactory = mediaSourceFactory as? DefaultMediaSourceFactory,
  )

  constructor(builder: ExoPlayer.Builder) : this(
    builder = builder,
    mediaSourceFactory = builder.mediaSourceFactorySupplier.get(),
  )
}
