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

package kohii.v2.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import kohii.v2.demo.DemoItemFragment.Companion.KEY_SEED
import kotlin.LazyThreadSafetyMode.NONE

class DemoHostActivity : AppCompatActivity() {

  private val demoItem: DemoItem by lazy(NONE) {
    requireNotNull(intent.getParcelableExtra(KEY_DEMO_ITEM))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    title = demoItem.title
    if (savedInstanceState == null) {
      supportFragmentManager.commit {
        replace(android.R.id.content, demoItem.fragment, bundleOf(KEY_SEED to demoItem.title))
      }
    }
  }

  companion object {
    private const val KEY_DEMO_ITEM = "KEY_DEMO_ITEM"

    fun Context.createIntent(demoItem: DemoItem): Intent =
      Intent(this, DemoHostActivity::class.java).apply {
        putExtra(KEY_DEMO_ITEM, demoItem)
      }
  }
}
