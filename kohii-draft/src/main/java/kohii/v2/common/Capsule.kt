package kohii.v2.common

/**
 * Singleton Holder
 */
open class Capsule<T : Any, in A>(
  creator: (A) -> T,
  onCreate: ((T) -> Unit) = { }
) {
  @Volatile private var instance: T? = null

  private var creator: ((A) -> T)? = creator
  private var onCreate: ((T) -> Unit)? = onCreate

  private fun getInstance(arg: A): T {
    val check = instance
    if (check != null) {
      return check
    }

    return synchronized(this) {
      val doubleCheck = instance
      if (doubleCheck != null) {
        doubleCheck
      } else {
        val created = requireNotNull(creator)(arg)
        requireNotNull(onCreate)(created)
        instance = created
        creator = null
        onCreate = null
        created
      }
    }
  }

  fun get(arg: A): T = getInstance(arg)
}
