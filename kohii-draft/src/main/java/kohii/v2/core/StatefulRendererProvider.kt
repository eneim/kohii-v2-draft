package kohii.v2.core

import androidx.lifecycle.LifecycleOwner

abstract class StatefulRendererProvider : RendererProvider() {

  protected open fun onClear(): Unit = Unit

  final override fun onDestroy(owner: LifecycleOwner): Unit = onClear()
}
