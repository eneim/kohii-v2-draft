package kohii.v2.core

import android.os.Parcelable
import kohii.v2.internal.BindRequest
import kohii.v2.internal.hexCode
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

class Request(
  private val bucket: Bucket,
  private val data: Any,
  private val playableTag: String? = null,
  private val trigger: Float = 0.65f,
) {

  /**
   * A [Request.Builder] can be serialized to Parcel and passed around.
   */
  @Parcelize
  class Builder(
    private val data: @RawValue Any,
    private var playableTag: String? = null,
    private var trigger: Float = 0.65f,
  ) : Parcelable {

    fun withPlayableTag(playableTag: String): Builder = apply { this.playableTag = playableTag }

    fun withTrigger(trigger: Float): Builder = apply { this.trigger = trigger }

    /**
     * Builds a [Request] that will be used in the provided [Bucket].
     */
    fun requestIn(bucket: Bucket): Request = Request(
      bucket = bucket,
      data = data,
      playableTag = playableTag,
      trigger = trigger
    )
  }

  /**
   * Creates a new [Builder] from the information of this [Request].
   */
  fun newBuilder(): Builder = Builder(data, playableTag, trigger)

  /**
   * Binds the request to a [container]. Returns a [RequestHandle] that can be used to wait for the
   * result of this request, or cancel it.
   */
  fun bind(
    container: Any,
    config: Playback.Config.() -> Unit = { /* no-op */ }
  ): RequestHandle {
    val request = BindRequest(
      home = bucket.manager.home,
      manager = bucket.manager,
      bucket = bucket,
      tag = playableTag ?: Home.NO_TAG,
      container = container,
      playable = fetchPlayable(),
      config = Playback.Config(trigger).apply(config)
    )
    return bucket.manager.home.enqueueRequest(container, request)
  }

  private fun fetchPlayable(): Playable {
    val home = bucket.manager.home
    val existingPlayable: Playable? = home.playables.entries
      .firstOrNull { it.value != Home.NO_TAG && it.value == playableTag }
      ?.key

    if (existingPlayable != null) {
      require(existingPlayable.data == data) {
        "A playable tag $playableTag is used by different data: $data and ${existingPlayable.data}"
      }
    }

    return existingPlayable ?: bucket.manager.home
      .requirePlayableCreator(data)
      .createPlayable(
        playableManager = bucket.manager.playableManager,
        data = data,
        tag = playableTag ?: Home.NO_TAG.hexCode()
      )
  }
}
