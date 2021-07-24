package kohii.v2.internal

import android.graphics.Rect
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.core.view.contains
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun View.awaitAttached() {
  if (isAttachedToWindow) return else suspendCancellableCoroutine<Unit> { continuation ->
    val listener = object : OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View?) {
        removeOnAttachStateChangeListener(this)
        continuation.resume(Unit)
      }

      override fun onViewDetachedFromWindow(v: View?) = Unit
    }

    continuation.invokeOnCancellation {
      removeOnAttachStateChangeListener(listener)
    }
    addOnAttachStateChangeListener(listener)
  }
}

internal val Rect.area: Int get() = width() * height()
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
