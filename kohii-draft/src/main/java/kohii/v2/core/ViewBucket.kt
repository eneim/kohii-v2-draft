/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v2.core

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.widget.NestedScrollView
import kohii.v2.internal.NestedScrollViewBucket
import kohii.v2.internal.ViewGroupBucket
import kohii.v2.internal.ViewGroupTwoThreeBucket
import kohii.v2.internal.isAncestorOf
import kohii.v2.internal.onNotNull

// TODO(eneim): figure out the way to use Instrumentation Test to verify the ViewBucket behavior.
/**
 * A base [Bucket] implementation where the [Bucket.root] is a [ViewGroup].
 */
abstract class ViewBucket(
  manager: Manager,
  protected open val rootView: ViewGroup
) : Bucket(
  manager = manager,
  root = rootView
) {

  private val containers = mutableSetOf<View>()

  private val rootViewAttachStateListener = object : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View?) = onStart()
    override fun onViewDetachedFromWindow(v: View?) = onStop()
  }

  private val containerAttachStateListener = object : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View?) = v.onNotNull(manager::onContainerAttached)
    override fun onViewDetachedFromWindow(v: View?) = v.onNotNull(manager::onContainerDetached)
  }

  private val containerLayoutChangeListener = OnLayoutChangeListener { v: View?,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int ->
    if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
      v.onNotNull(manager::onContainerUpdated)
    }
  }

  @CallSuper
  override fun onAdd() {
    super.onAdd()
    if (rootView.isAttachedToWindow) {
      rootViewAttachStateListener.onViewAttachedToWindow(rootView)
    }
    rootView.addOnAttachStateChangeListener(rootViewAttachStateListener)
  }

  @CallSuper
  override fun onRemove() {
    super.onRemove()
    rootView.removeOnAttachStateChangeListener(rootViewAttachStateListener)
    containers.onEach(::onRemoveContainer).clear()
  }

  override fun accepts(container: Any): Boolean {
    return container is View && rootView.isAncestorOf(container)
  }

  override fun addContainer(container: Any) {
    super.addContainer(container)
    requireNotNull(container as? View).let { view: View ->
      if (containers.add(view)) {
        if (view.isAttachedToWindow) {
          containerAttachStateListener.onViewAttachedToWindow(view)
        }
        view.addOnAttachStateChangeListener(containerAttachStateListener)
        view.addOnLayoutChangeListener(containerLayoutChangeListener)
      }
    }
  }

  override fun removeContainer(container: Any) {
    super.removeContainer(container)
    requireNotNull(container as? View)
      .takeIf(containers::remove)
      ?.let(::onRemoveContainer)
  }

  private fun onRemoveContainer(container: View) {
    container.removeOnLayoutChangeListener(containerLayoutChangeListener)
    container.removeOnAttachStateChangeListener(containerAttachStateListener)
    manager.onContainerRemoved(container)
  }

  companion object {

    internal operator fun get(manager: Manager, root: ViewGroup): ViewBucket = when {
      root is NestedScrollView -> NestedScrollViewBucket(manager, root)
      SDK_INT >= VERSION_CODES.M -> ViewGroupTwoThreeBucket(manager, root)
      else -> ViewGroupBucket(manager, root)
    }
  }
}
