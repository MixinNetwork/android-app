package one.mixin.android.net

import kotlinx.coroutines.delay
import one.mixin.android.api.MixinResponse
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> executeWithRetry(
    maxRetries: Int,
    executeFunc: suspend () -> MixinResponse<T>,
    recoverFunc: suspend () -> MixinResponse<T>
): MixinResponse<T> {
    return try {
        val response = executeFunc()
        if (maxRetries > 0 && response.errorCode == 500) {
            delay(200)
            executeWithRetry(maxRetries - 1, executeFunc, recoverFunc)
        } else {
            response
        }
    } catch (socketTimeoutException: SocketTimeoutException) {
        // When Timeout, try to get data
        val response = recoverFunc()
        if (response.isSuccess) {
            response
        } else {
            executeWithRetry(maxRetries - 1, executeFunc, recoverFunc)
        }
    } catch (ioException: IOException) {
        if (maxRetries > 0) {
            executeWithRetry(maxRetries - 1, executeFunc, recoverFunc)
        } else {
            throw ioException
        }
    }
}

