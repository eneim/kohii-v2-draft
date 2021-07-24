package kohii.v2.internal

import android.os.Build.VERSION_CODES
import android.view.View
import android.view.View.OnScrollChangeListener
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import kohii.v2.common.logDebug
import kohii.v2.core.Manager
import kohii.v2.core.ViewBucket

@RequiresApi(VERSION_CODES.M)
internal class ViewGroupTwoThreeBucket(
  manager: Manager,
  rootView: ViewGroup
) : ViewBucket(
  manager = manager,
  rootView = rootView
), OnScrollChangeListener {

  override fun onAdd() {
    super.onAdd()
    rootView.setOnScrollChangeListener(this)
  }

  override fun onRemove() {
    super.onRemove()
    rootView.setOnScrollChangeListener(null)
  }

  override fun onScrollChange(
    v: View?,
    scrollX: Int,
    scrollY: Int,
    oldScrollX: Int,
    oldScrollY: Int
  ) {
    "Bucket_REFRESH [x: $oldScrollX → $scrollX, y: $oldScrollY → $scrollY] b=$this".logDebug()
    manager.refresh()
  }
}
