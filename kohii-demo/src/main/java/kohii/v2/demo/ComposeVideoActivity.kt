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

package kohii.v2.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import kohii.v2.demo.R.string

@ExperimentalComposeUiApi
class ComposeVideoActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        LazyColumn {
          /* StyledVideoPlayer(
            mediaItems = listOf(
              MediaItem.Builder().setUri(MainActivity.DemoUrl).build()
            ),
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(16 / 9f)
          ) */
          item {
            Card(
              modifier = Modifier
                .onGloballyPositioned {
                  Log.i("PlayerState#Video", "layout: ${it.boundsInWindow()}")
                }
            ) {
              Image(
                painter = rememberImagePainter(data = "https://picsum.photos/id/1002/4312/2868"),
                contentDescription = "Coil image.",
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 10.dp),
                contentScale = ContentScale.FillWidth
              )

              DisposableEffect("") {
                onDispose {
                  Log.w("PlayerState", "disposed")
                }
              }
            }
          }

          item {
            Text(
              text = stringResource(id = string.long_text),
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      }
    }
  }
}
