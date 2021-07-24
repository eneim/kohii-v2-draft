package kohii.v2.internal

import android.view.ViewGroup
import android.view.ViewTreeObserver.OnScrollChangedListener
import kohii.v2.core.Manager
import kohii.v2.core.ViewBucket

internal class ViewGroupBucket(
  manager: Manager,
  rootView: ViewGroup
) : ViewBucket(
  manager = manager,
  rootView = rootView
) {

  private val globalScrollChangeListener = OnScrollChangedListener(manager::refresh)

  override fun onAdd() {
    super.onAdd()
    rootView.viewTreeObserver.addOnScrollChangedListener(globalScrollChangeListener)
  }

  override fun onRemove() {
    super.onRemove()
    rootView.viewTreeObserver.removeOnScrollChangedListener(globalScrollChangeListener)
  }
}
