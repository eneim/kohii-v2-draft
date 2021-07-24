package kohii.v2.internal

import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import kohii.v2.core.Manager
import kohii.v2.core.ViewBucket

class NestedScrollViewBucket(
  manager: Manager,
  override val rootView: NestedScrollView
) : ViewBucket(
  manager,
  rootView
), OnScrollChangeListener {

  override fun onScrollChange(
    v: NestedScrollView?,
    scrollX: Int,
    scrollY: Int,
    oldScrollX: Int,
    oldScrollY: Int
  ) {
    manager.refresh()
  }

  override fun onAdd() {
    super.onAdd()
    rootView.setOnScrollChangeListener(this)
  }

  override fun onRemove() {
    super.onRemove()
    rootView.setOnScrollChangeListener(null as OnScrollChangeListener?)
  }
}
