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

package kohii.v2.core

import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kohii.v2.R
import kohii.v2.internal.NestedScrollViewBucket
import kohii.v2.internal.RecyclerViewBucket
import kohii.v2.internal.ViewGroupBucket
import kohii.v2.internal.ViewGroupTwoThreeBucket
import kohii.v2.internal.ViewPager2Bucket
import kohii.v2.internal.getTagOrPut
import kohii.v2.internal.getTypedTag
import kohii.v2.internal.isAncestorOf

// TODO(eneim): figure out the way to use Instrumentation Test to verify the ViewBucket behavior.
/**
 * A base [Bucket] implementation where the [Bucket.root] is a [ViewGroup].
 */
abstract class ViewBucket(
  manager: Manager,
  protected open val rootView: ViewGroup,
) : Bucket(
  manager = manager,
  root = rootView
) {

  protected abstract val axis: Axis
  private val containers = mutableSetOf<Any>()

  private val rootViewAttachStateListener = object : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View?) = onStart()
    override fun onViewDetachedFromWindow(v: View?) = onStop()
  }

  private val containerAttachStateListener = object : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View?) {
      v?.let(manager::onContainerAttached)
    }

    override fun onViewDetachedFromWindow(v: View?) {
      v?.let(manager::onContainerDetached)
    }
  }

  private val containerLayoutChangeListener =
    OnLayoutChangeListener { v: View?, nl: Int, nt: Int, nr: Int, nb: Int, ol: Int, ot: Int, or: Int, ob: Int ->
      if (nl != ol || nr != or || nt != ot || nb != ob) {
        v?.let(manager::onContainerUpdated)
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

  override fun accept(container: Any): Boolean {
    return container is View && container.isAttachedToWindow && rootView.isAncestorOf(container)
  }

  override fun selectToPlayInternal(candidates: Collection<Playback>): Collection<Playback> {
    return candidates.sortedWith(object : Comparator<Playback> {
      override fun compare(
        left: Playback?,
        right: Playback?,
      ): Int {
        if (left == null && right == null) return 0
        if (left == null) return -1
        if (right == null) return 1
        return left.token.compare(right.token, axis)
      }
    })
  }

  override fun addContainer(container: Any) {
    super.addContainer(container)
    if (containers.add(container)) {
      if (container is View) {
        if (container.isAttachedToWindow) {
          containerAttachStateListener.onViewAttachedToWindow(container)
        }
        container.addOnAttachStateChangeListener(containerAttachStateListener)
        container.addOnLayoutChangeListener(containerLayoutChangeListener)
      } else {
        throw IllegalArgumentException("Container must be a View.")
      }
    }
  }

  override fun removeContainer(container: Any) {
    super.removeContainer(container)
    if (containers.remove(container)) {
      onRemoveContainer(container)
    }
  }

  private fun onRemoveContainer(container: Any) {
    if (container is View) {
      container.removeOnLayoutChangeListener(containerLayoutChangeListener)
      container.removeOnAttachStateChangeListener(containerAttachStateListener)
    }
    manager.onContainerRemoved(container)
  }

  companion object {

    internal operator fun get(
      manager: Manager,
      root: ViewGroup,
    ): ViewBucket = when {
      root is ViewPager2 -> ViewPager2Bucket(manager, root)
      root is NestedScrollView -> NestedScrollViewBucket(manager, root)
      root is RecyclerView -> RecyclerViewBucket(manager, root)
      SDK_INT >= 23 /* VERSION_CODES.M */ -> ViewGroupTwoThreeBucket(manager, root)
      else -> ViewGroupBucket(manager, root)
    }
  }
}

internal fun View.fetchContainersTag(): MutableList<View>? =
  getTypedTag(R.id.container_recycler_item_views)

internal fun View.getContainersTag(): MutableList<View> =
  getTagOrPut(R.id.container_recycler_item_views) { mutableListOf() }

internal fun View.removeContainersTag(): Unit = setTag(R.id.container_recycler_item_views, null)
