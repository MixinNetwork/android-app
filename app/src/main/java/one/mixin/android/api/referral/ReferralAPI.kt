package one.mixin.android.api.referral

import one.mixin.android.Constants.RouteConfig.REFERRAL_BOT_USER_ID
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.BotPublicKey
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.UserSession
import one.mixin.android.util.ErrorHandler
import retrofit2.Response

private suspend fun <R> retryReferralRequestAfterRefreshingSession(
    requestSession: suspend (List<String>) -> MixinResponse<List<UserSession>>,
    retryRequest: suspend () -> R?,
): R? {
    return handleMixinResponse(
        invokeNetwork = {
            try {
                requireNotNull(BotPublicKey.forBot(REFERRAL_BOT_USER_ID)).get(requestSession, force = true)
                MixinResponse<Unit>()
            } catch (e: MixinResponseException) {
                MixinResponse<Unit>(ResponseError(e.errorCode, e.errorCode, e.errorDescription))
            }
        },
        successBlock = { retryRequest() },
    )
}

private suspend fun <T, R, N> requestReferralAPIInternal(
    invokeNetwork: suspend () -> N,
    successBlock: (suspend (N) -> R)? = null,
    failureBlock: (suspend (N) -> Boolean)? = null,
    exceptionBlock: (suspend (t: Throwable) -> Boolean)? = null,
    doAfterNetworkSuccess: (() -> Unit)? = null,
    defaultErrorHandle: (suspend (N) -> Unit),
    defaultExceptionHandle: (suspend (t: Throwable) -> Unit) = {
        ErrorHandler.handleError(it)
    },
    endBlock: (() -> Unit)? = null,
    authErrorRetryCount: Int = 1,
    requestSession: suspend (List<String>) -> MixinResponse<List<UserSession>>,
    isSuccessful: (N) -> Boolean,
    getErrorCode: (N) -> Int,
    retryRequest: suspend (Int) -> R?,
): R? {
    val response =
        try {
            invokeNetwork()
        } catch (t: Throwable) {
            if (exceptionBlock?.invoke(t) != true) {
                defaultExceptionHandle.invoke(t)
            }
            endBlock?.invoke()
            return null
        }

    doAfterNetworkSuccess?.invoke()

    return if (isSuccessful(response)) {
        val result = successBlock?.invoke(response)
        endBlock?.invoke()
        result
    } else {
        if (getErrorCode(response) == ErrorHandler.AUTHENTICATION && authErrorRetryCount > 0) {
            return retryReferralRequestAfterRefreshingSession(
                requestSession = requestSession,
                retryRequest = {
                    retryRequest(authErrorRetryCount - 1)
                },
            )
        }
        if (failureBlock?.invoke(response) != true) {
            defaultErrorHandle(response)
        }
        endBlock?.invoke()
        null
    }
}

suspend fun <T, R> requestReferralMixinAPI(
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
): R? {
    return requestReferralAPIInternal<T, R, MixinResponse<T>>(
        invokeNetwork = invokeNetwork,
        successBlock = successBlock,
        failureBlock = failureBlock,
        exceptionBlock = exceptionBlock,
        doAfterNetworkSuccess = doAfterNetworkSuccess,
        defaultErrorHandle = defaultErrorHandle,
        defaultExceptionHandle = defaultExceptionHandle,
        endBlock = endBlock,
        authErrorRetryCount = authErrorRetryCount,
        requestSession = requestSession,
        isSuccessful = { it.isSuccess },
        getErrorCode = { it.errorCode },
        retryRequest = { retryCount ->
            requestReferralMixinAPI(
                invokeNetwork = invokeNetwork,
                successBlock = successBlock,
                failureBlock = failureBlock,
                exceptionBlock = exceptionBlock,
                doAfterNetworkSuccess = doAfterNetworkSuccess,
                defaultErrorHandle = defaultErrorHandle,
                defaultExceptionHandle = defaultExceptionHandle,
                endBlock = endBlock,
                authErrorRetryCount = retryCount,
                requestSession = requestSession,
            )
        },
    )
}

suspend fun <T, R> requestReferralAPI(
    invokeNetwork: suspend () -> Response<T>,
    successBlock: (suspend (Response<T>) -> R)? = null,
    failureBlock: (suspend (Response<T>) -> Boolean)? = null,
    exceptionBlock: (suspend (t: Throwable) -> Boolean)? = null,
    doAfterNetworkSuccess: (() -> Unit)? = null,
    defaultErrorHandle: (suspend (Response<T>) -> Unit) = {
        ErrorHandler.handleError(Throwable("Referral request failed with code ${it.code()}"))
    },
    defaultExceptionHandle: (suspend (t: Throwable) -> Unit) = {
        ErrorHandler.handleError(it)
    },
    endBlock: (() -> Unit)? = null,
    authErrorRetryCount: Int = 1,
    requestSession: suspend (List<String>) -> MixinResponse<List<UserSession>>,
): R? {
    return requestReferralAPIInternal<T, R, Response<T>>(
        invokeNetwork = invokeNetwork,
        successBlock = successBlock,
        failureBlock = failureBlock,
        exceptionBlock = exceptionBlock,
        doAfterNetworkSuccess = doAfterNetworkSuccess,
        defaultErrorHandle = defaultErrorHandle,
        defaultExceptionHandle = defaultExceptionHandle,
        endBlock = endBlock,
        authErrorRetryCount = authErrorRetryCount,
        requestSession = requestSession,
        isSuccessful = { it.isSuccessful },
        getErrorCode = { it.code() },
        retryRequest = { retryCount ->
            requestReferralAPI(
                invokeNetwork = invokeNetwork,
                successBlock = successBlock,
                failureBlock = failureBlock,
                exceptionBlock = exceptionBlock,
                doAfterNetworkSuccess = doAfterNetworkSuccess,
                defaultErrorHandle = defaultErrorHandle,
                defaultExceptionHandle = defaultExceptionHandle,
                endBlock = endBlock,
                authErrorRetryCount = retryCount,
                requestSession = requestSession,
            )
        },
    )
}
