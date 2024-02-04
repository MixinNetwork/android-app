package one.mixin.android.net

import kotlinx.coroutines.delay
import one.mixin.android.api.MixinResponse
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> executeWithRetry(
    retries: Int,
    executeFunc: suspend () -> MixinResponse<T>,
    getExecuteFunc: suspend () -> MixinResponse<T>,
): MixinResponse<T> {
    return try {
        val response = executeFunc()
        if (retries > 0 && response.errorCode == 500) {
            delay(200)
            executeWithRetry(retries - 1, executeFunc, getExecuteFunc)
        } else {
            response
        }
    } catch (e: SocketTimeoutException) {
        val response = getExecuteFunc()
        if (response.isSuccess) {
            response
        } else {
            executeWithRetry(retries - 1, executeFunc, getExecuteFunc)
        }
    } catch (e: IOException) {
        if (retries > 0) {
            executeWithRetry(retries - 1, executeFunc, getExecuteFunc)
        } else {
            throw e
        }
    }
}
