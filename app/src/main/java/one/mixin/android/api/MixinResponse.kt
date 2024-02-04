package one.mixin.android.api

import kotlinx.coroutines.Dispatchers
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

    constructor(error:ResponseError) : this() {
        this.error = error
    }

    constructor(response: Throwable) : this() {
        error = ResponseError(500, 500, response.message ?: "")
    }

    var data: T? = null
    var error: ResponseError? = null
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
    switchContext: CoroutineContext = Dispatchers.IO,
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
): R? {
    val response =
        try {
            withContext(switchContext) {
                invokeNetwork()
            }
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
        if (failureBlock?.invoke(response) != true) {
            defaultErrorHandle(response)
        }
        endBlock?.invoke()
        null
    }
}
