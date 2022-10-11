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

package kohii.v2.demo.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.contains
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

inline fun BottomSheetBehavior<*>.doOnStateChanged(
  crossinline onStateChanged: (newState: Int) -> Unit,
) = addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
  override fun onStateChanged(
    bottomSheet: View,
    newState: Int,
  ) = onStateChanged(newState)

  override fun onSlide(
    bottomSheet: View,
    slideOffset: Float,
  ) = Unit
})

inline fun BottomSheetDialog.doOnStateChanged(
  crossinline onStateChanged: (newState: Int) -> Unit,
) = behavior.doOnStateChanged(onStateChanged)

/**
 * Given a Flow A, returns a [Flow] that emits a pair of the latest value emitted by A (as the
 * second value of the pair), and the previous value emitted before (as the first value of the
 * pair).
 */
fun <T : Any?> Flow<T>.flowWithPrevious(): Flow<Pair<T?, T>> = flow {
  var previousValue: T? = null
  collect { value: T ->
    val newPair = previousValue to value
    previousValue = value
    emit(newPair)
  }
}

internal fun <V : View> ViewGroup.isAncestorOf(view: V): Boolean {
  if (view === this || this.contains(view)) return true
  var target: View = view
  var parent = target.parent
  while (parent != null && parent !== this && parent is View) {
    target = parent
    parent = target.parent
  }
  return parent === this
}

fun Window.hideSystemBars() {
  val windowInsetsController = WindowCompat.getInsetsController(this, decorView)
  // Configure the behavior of the hidden system bars
  windowInsetsController.systemBarsBehavior =
    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  // Hide both the status bar and the navigation bar
  windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
}

fun Window.showSystemBars() {
  val windowInsetsController = WindowCompat.getInsetsController(this, decorView)
  // Configure the behavior of the hidden system bars
  windowInsetsController.systemBarsBehavior =
    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  // Hide both the status bar and the navigation bar
  windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
}

val ViewGroup.layoutInflater: LayoutInflater get() = LayoutInflater.from(context)
