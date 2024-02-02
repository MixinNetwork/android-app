package one.mixin.android.net

import kotlinx.coroutines.delay
import one.mixin.android.api.MixinResponse

suspend fun <T> executeWithRetry(
    retries: Int,
    executeFunc: suspend () -> MixinResponse<T>,
): MixinResponse<T> {
    return try {
        val response = executeFunc()
        if (retries > 0 && response.errorCode == 500) {
            delay(200)
            executeWithRetry(retries - 1, executeFunc)
        } else {
            response
        }
    } catch (e: Exception) {
        if (retries > 0) {
            executeWithRetry(retries - 1, executeFunc)
        } else {
            throw e
        }
    }
}
