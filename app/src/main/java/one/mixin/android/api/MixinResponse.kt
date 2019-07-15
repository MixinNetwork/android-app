package one.mixin.android.api

import kotlinx.coroutines.withContext
import one.mixin.android.util.ErrorHandler
import retrofit2.Response
import kotlin.coroutines.CoroutineContext

class MixinResponse<T>() {

    constructor(response: Response<T>) : this() {
        if (response.isSuccessful) {
            data = response.body()
        } else {
            error = ResponseError(response.code(), response.code(), response.errorBody().toString())
        }
    }

    constructor(response: Throwable) : this() {
        error = ResponseError(500, 500, response.message ?: "")
    }

    var data: T? = null
    var error: ResponseError? = null
    var prev: String? = null
    var next: String? = null

    val isSuccess: Boolean
        get() = error == null

    val errorCode: Int
        get() = if (error != null) error!!.code else 0

    val errorDescription: String
        get() = if (error != null) error!!.description else ""
}

suspend fun <T, R> handleMixinResponse(
    invokeNetwork: suspend () -> MixinResponse<T>,
    switchContext: CoroutineContext? = null,
    successBlock: (suspend (MixinResponse<T>) -> R)? = null,
    failureBlock: (suspend (MixinResponse<T>) -> Boolean)? = null,
    exceptionBlock: ((t: Throwable) -> Boolean)? = null,
    doAfterNetworkSuccess: (() -> Unit)? = null,
    defaultErrorHandle: (suspend (MixinResponse<T>) -> Unit) = {
        ErrorHandler.handleMixinError(it.errorCode)
    },
    defaultExceptionHandle: (suspend (t: Throwable) -> Unit) = {
        ErrorHandler.handleError(it)
    }
) {
    val response = if (switchContext != null) {
        try {
            withContext(switchContext) {
                invokeNetwork()
            }
        } catch (t: Throwable) {
            if (exceptionBlock?.invoke(t) != true) {
                defaultExceptionHandle.invoke(t)
            }
            return
        }
    } else {
        try {
            invokeNetwork()
        } catch (t: Throwable) {
            if (exceptionBlock?.invoke(t) != true) {
                defaultExceptionHandle.invoke(t)
            }
            return
        }
    }

    doAfterNetworkSuccess?.invoke()

    if (response.isSuccess) {
        successBlock?.invoke(response)
    } else {
        if (failureBlock?.invoke(response) != true) {
            defaultErrorHandle(response)
        }
    }
}