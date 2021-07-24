package kohii.v2.internal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kohii.v2.common.logWarn
import kohii.v2.core.Home
import kohii.v2.core.Playback
import kohii.v2.core.RequestHandle
import kotlinx.coroutines.Deferred

/**
 * An implementation of the [RequestHandle] that automatically remove the request once it is done,
 * or the lifecycle that requested this is destroyed.
 */
internal class RequestHandleImpl(
  private val home: Home,
  private val container: Any,
  private val lifecycle: Lifecycle,
  private val deferred: Deferred<Result<Playback>>,
) : RequestHandle, DefaultLifecycleObserver {

  init {
    lifecycle.addObserver(this)
    deferred.invokeOnCompletion {
      "Request completes with throwable: $it".logWarn()
      onCompleted()
    }
  }

  override fun onDestroy(owner: LifecycleOwner) {
    cancel()
    onCompleted()
  }

  override fun cancel() {
    lifecycle.removeObserver(this)
    deferred.cancel()
  }

  override suspend fun result(): Result<Playback> = try {
    deferred.await()
  } catch (error: Throwable) {
    Result.failure(error)
  }

  private fun onCompleted() {
    lifecycle.removeObserver(this)
    val handle = home.requests[container]
    if (handle === this) home.requests.remove(container)
  }
}
