package one.mixin.android.api.referral

import one.mixin.android.Constants.Account.PREF_REFERRAL_BOT_PK
import one.mixin.android.Constants.RouteConfig.REFERRAL_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.UserSession
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId
import retrofit2.Response

private suspend fun persistReferralBotSession(sessionData: UserSession) {
    val account = requireNotNull(Session.getAccount()) { "Account is required for database access." }
    val accountId = requireNotNull(Session.getAccountId()) { "Account id is required for referral session access." }

    MixinApplication.appContext.defaultSharedPreferences.putString(PREF_REFERRAL_BOT_PK, sessionData.publicKey)
    MixinDatabase.getDatabase(MixinApplication.appContext, account.identityNumber)
        .participantSessionDao()
        .insertSuspend(
            ParticipantSession(
                conversationId = generateConversationId(sessionData.userId, accountId),
                userId = sessionData.userId,
                sessionId = sessionData.sessionId,
                publicKey = sessionData.publicKey,
            ),
        )
}

private suspend fun <R> retryReferralRequestAfterRefreshingSession(
    requestSession: suspend (List<String>) -> MixinResponse<List<UserSession>>,
    retryRequest: suspend () -> R?,
): R? {
    return handleMixinResponse(
        invokeNetwork = { requestSession(listOf(REFERRAL_BOT_USER_ID)) },
        successBlock = { response ->
            val sessionData = requireNotNull(response.data).first()
            persistReferralBotSession(sessionData)
            retryRequest()
        },
    )
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

    return if (response.isSuccess) {
        val result = successBlock?.invoke(response)
        endBlock?.invoke()
        result
    } else {
        if (response.errorCode == ErrorHandler.AUTHENTICATION && authErrorRetryCount > 0) {
            return retryReferralRequestAfterRefreshingSession(
                requestSession = requestSession,
                retryRequest = {
                    requestReferralMixinAPI(
                        invokeNetwork = invokeNetwork,
                        successBlock = successBlock,
                        failureBlock = failureBlock,
                        exceptionBlock = exceptionBlock,
                        doAfterNetworkSuccess = doAfterNetworkSuccess,
                        defaultErrorHandle = defaultErrorHandle,
                        defaultExceptionHandle = defaultExceptionHandle,
                        endBlock = endBlock,
                        authErrorRetryCount = authErrorRetryCount - 1,
                        requestSession = requestSession,
                    )
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

    return if (response.isSuccessful) {
        val result = successBlock?.invoke(response)
        endBlock?.invoke()
        result
    } else {
        if (response.code() == ErrorHandler.AUTHENTICATION && authErrorRetryCount > 0) {
            return retryReferralRequestAfterRefreshingSession(
                requestSession = requestSession,
                retryRequest = {
                    requestReferralAPI(
                        invokeNetwork = invokeNetwork,
                        successBlock = successBlock,
                        failureBlock = failureBlock,
                        exceptionBlock = exceptionBlock,
                        doAfterNetworkSuccess = doAfterNetworkSuccess,
                        defaultErrorHandle = defaultErrorHandle,
                        defaultExceptionHandle = defaultExceptionHandle,
                        endBlock = endBlock,
                        authErrorRetryCount = authErrorRetryCount - 1,
                        requestSession = requestSession,
                    )
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
