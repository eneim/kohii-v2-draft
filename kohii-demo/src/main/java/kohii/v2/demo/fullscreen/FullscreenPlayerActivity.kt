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

package kohii.v2.demo.fullscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import kohii.v2.core.ExoPlayerEngine
import kohii.v2.core.Playback
import kohii.v2.core.PlayerEventListener
import kohii.v2.core.Request
import kohii.v2.demo.common.hideSystemBars
import kohii.v2.demo.databinding.FragmentFullscreenSheetBinding

class FullscreenPlayerActivity : AppCompatActivity() {

  // Note: this code is for demonstration purpose only. In practice, if an Activity is registered
  // as singleTop, `onNewIntent` might be called and and the request value is changed.
  private val request: Request by lazy {
    requireNotNull(intent.extras?.getParcelable(ARGS_REQUEST))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding: FragmentFullscreenSheetBinding =
      FragmentFullscreenSheetBinding.inflate(layoutInflater)
    setContentView(binding.root)
    window.hideSystemBars()

    val engine = ExoPlayerEngine()
    engine.useBucket(binding.root)
    engine.setUp(request).bind(container = binding.videoContainer) {
      addPlayerEventListener(object : PlayerEventListener {
        override fun onStateChanged(
          playback: Playback,
          state: Int,
        ) {
          if (state == Player.STATE_ENDED) finish()
        }
      })
    }
  }

  override fun finish() {
    setResult(RESULT_OK, Intent().putExtra(ARGS_REQUEST, request))
    super.finish()
  }

  companion object {
    const val ARGS_REQUEST = "ARGS_REQUEST"

    fun Context.newPlayerIntent(request: Request): Intent =
      Intent(this, FullscreenPlayerActivity::class.java).apply {
        putExtras(bundleOf(ARGS_REQUEST to request))
      }
  }
}
