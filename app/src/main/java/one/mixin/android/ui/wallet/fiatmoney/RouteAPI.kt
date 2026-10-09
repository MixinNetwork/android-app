package one.mixin.android.ui.wallet.fiatmoney

import kotlinx.coroutines.CancellationException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.RouteBotPublicKey
import one.mixin.android.api.response.UserSession
import one.mixin.android.util.ErrorHandler

suspend fun <T, R> requestRouteAPI(
    invokeNetwork: suspend () -> MixinResponse<T>,
    successBlock: (suspend (MixinResponse<T>) -> R)? = null,
    failureBlock: (suspend (MixinResponse<T>) -> Boolean)? = null,
    exceptionBlock: (suspend (t: Throwable) -> Boolean)? = null,
    doAfterNetworkSuccess: (() -> Unit)? = null,
    defaultErrorHandle: (suspend (MixinResponse<T>) -> Unit) = {
        ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
    },
    defaultExceptionHandle: (suspend (t: Throwable) -> Unit) = {
        ErrorHandler.handleError(it)
    },
    endBlock: (() -> Unit)? = null,
    authErrorRetryCount: Int = 1,
    requestSession: suspend (List<String>) -> MixinResponse<List<UserSession>>,
    publicKey: RouteBotPublicKey = RouteBotPublicKey.shared,
): R? {
    try {
        var retries = authErrorRetryCount
        var force = false
        while (true) {
            var synchronized = false
            val response = try {
                publicKey.get(requestSession, force)
                synchronized = true
                invokeNetwork()
            } catch (t: CancellationException) {
                throw t
            } catch (t: MixinResponseException) {
                MixinResponse<T>(ResponseError(t.errorCode, t.errorCode, t.errorDescription))
            } catch (t: Throwable) {
                if (exceptionBlock?.invoke(t) != true) defaultExceptionHandle(t)
                return null
            }
            if (synchronized) doAfterNetworkSuccess?.invoke()
            if (response.isSuccess) return successBlock?.invoke(response)
            if (synchronized && response.errorCode == ErrorHandler.AUTHENTICATION && retries > 0) {
                retries--
                force = true
                continue
            }
            if (failureBlock?.invoke(response) != true) defaultErrorHandle(response)
            return null
        }
    } finally {
        endBlock?.invoke()
    }
}
