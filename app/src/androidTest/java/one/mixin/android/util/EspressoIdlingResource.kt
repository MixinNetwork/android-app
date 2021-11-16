package one.mixin.android.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = SimpleCountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

inline fun <T> wrapEspressoIdlingResource(function: () -> T): T {
    // Espresso does not work well with coroutines yet. See
    // https://github.com/Kotlin/kotlinx.coroutines/issues/982
    EspressoIdlingResource.increment() // Set app as busy.
    return try {
        function()
    } finally {
        EspressoIdlingResource.decrement() // Set app as idle.
    }
}

fun waitMillis(millis: Long) {
    EspressoIdlingResource.increment()
    val job = MixinApplication.appScope.launch {
        delay(millis)
    }
    job.invokeOnCompletion {
        EspressoIdlingResource.decrement()
    }
}
