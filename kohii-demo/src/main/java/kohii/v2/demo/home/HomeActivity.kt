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

package kohii.v2.demo.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.epoxy.EpoxyRecyclerView
import kohii.v2.demo.home.DemoHostActivity.Companion.createIntent
import kohii.v2.demo.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

  private val viewModel: HomeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding: ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.demoItems.setItemSpacingDp(8)

    viewModel.demoItems.observe(this) { items ->
      binding.demoItems.bindItems(items)
    }
  }

  private fun EpoxyRecyclerView.bindItems(items: List<DemoItem>) = withModels {
    items.forEachIndexed { _, demoItem ->
      demoItem {
        id(demoItem.hashCode())
        data(demoItem)
        onClick { view, demoItem ->
          startActivity(view.context.createIntent(demoItem))
        }
      }
    }
  }
}
