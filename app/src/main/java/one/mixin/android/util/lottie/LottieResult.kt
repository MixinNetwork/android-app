package one.mixin.android.util.lottie

class LottieResult<V>(val value: V? = null, val exception: Throwable? = null) {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        if (other !is LottieResult<*>) return false

        if (value != null && value == other.value) return true

        if (exception != null && exception == other.exception) return true

        return false
    }

    override fun hashCode(): Int =
        arrayOf(value, exception).contentHashCode()
}

interface LottieListener<T> {
    fun onResult(result: T)
}
