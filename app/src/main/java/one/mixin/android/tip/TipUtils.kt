package one.mixin.android.tip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.NotEnoughPartialsException
import one.mixin.android.tip.exception.PinIncorrectException
import one.mixin.android.tip.exception.TipException
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.reportException

suspend fun <T> tipNetwork(network: suspend () -> MixinResponse<T>): Result<T> {
    return withContext(Dispatchers.IO) {
        val response = network.invoke()
        val data = response.data
        if (response.isSuccess && data != null) {
            return@withContext Result.success(data)
        } else {
            return@withContext Result.failure(
                TipNetworkException(requireNotNull(response.error)),
            )
        }
    }
}

suspend fun <T> tipNetworkNullable(network: suspend () -> MixinResponse<T>): Result<T?> {
    return withContext(Dispatchers.IO) {
        val response = network.invoke()
        if (response.isSuccess) {
            return@withContext Result.success(response.data)
        } else {
            return@withContext Result.failure(
                TipNetworkException(requireNotNull(response.error)),
            )
        }
    }
}

fun Throwable.getTipExceptionMsg(context: Context, nodeFailedInfo: String? = null): String {
    var msg = when (this) {
        is PinIncorrectException -> context.getString(R.string.PIN_incorrect)
        is NotEnoughPartialsException -> this.getMsg(context)
        is NotAllSignerSuccessException -> this.getMsg(context)
        is DifferentIdentityException -> context.getString(R.string.PIN_not_same_as_last_time)
        is TipNetworkException -> this.error.run {
            context.getMixinErrorStringByCode(this.code, this.description)
        }
        else -> {
            "${context.getString(R.string.Set_or_update_PIN_failed)}\n${this.getStackTraceString()}"
        }
    }
    msg = if (nodeFailedInfo.isNullOrBlank().not()) {
        nodeFailedInfo + "\n"
    } else {
        ""
    } + msg

    reportException(TipException(msg))

    return msg
}

fun NotEnoughPartialsException.getMsg(context: Context): String =
    when (tipNodeError) {
        is TooManyRequestError -> context.getString(R.string.error_too_many_request)
        is IncorrectPinError -> context.getString(R.string.PIN_incorrect)
        else -> context.getString(R.string.Not_enough_partials)
    }

fun NotAllSignerSuccessException.getMsg(context: Context): String =
    if (tipNodeError is TooManyRequestError) {
        context.getString(R.string.error_too_many_request)
    } else if (tipNodeError is IncorrectPinError) {
        context.getString(R.string.PIN_incorrect)
    } else {
        if (allFailure()) {
            context.getString(R.string.All_signer_failure)
        } else {
            "${context.getString(R.string.Not_all_signer_success)}\n${this.getStackTraceString()}"
        }
    }
