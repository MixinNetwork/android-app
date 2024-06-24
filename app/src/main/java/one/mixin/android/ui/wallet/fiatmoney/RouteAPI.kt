package one.mixin.android.ui.wallet.fiatmoney

import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
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
        val r = successBlock?.invoke(response)
        endBlock?.invoke()
        r
    } else {
        if (response.errorCode == ErrorHandler.AUTHENTICATION && authErrorRetryCount > 0) {
            return handleMixinResponse(
                invokeNetwork = { requestSession(listOf(ROUTE_BOT_USER_ID)) },
                successBlock = { resp ->
                    val sessionData = requireNotNull(resp.data)[0]
                    MixinApplication.appContext.defaultSharedPreferences.putString(PREF_ROUTE_BOT_PK, sessionData.publicKey)
                    MixinDatabase.getDatabase(MixinApplication.appContext).participantSessionDao().insertSuspend(ParticipantSession(generateConversationId(sessionData.userId, Session.getAccountId()!!), sessionData.userId, sessionData.sessionId, publicKey = sessionData.publicKey))
                    return@handleMixinResponse requestRouteAPI(invokeNetwork, successBlock, failureBlock, exceptionBlock, doAfterNetworkSuccess, defaultErrorHandle, defaultExceptionHandle, endBlock, authErrorRetryCount - 1, requestSession)
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
