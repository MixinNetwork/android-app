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
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.util.reportException

suspend fun <T> tipNetwork(network: suspend () -> MixinResponse<T>): Result<T> {
    return withContext(Dispatchers.IO) {
        val response = network.invoke()
        val data = response.data
        if (response.isSuccess && data != null) {
            return@withContext Result.success(data)
        } else {
            return@withContext Result.failure(
                TipNetworkException(requireNotNull(response.error))
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
                TipNetworkException(requireNotNull(response.error))
            )
        }
    }
}

fun Throwable.getTipExceptionMsg(context: Context): String =
    when (this) {
        is PinIncorrectException -> context.getString(R.string.PIN_incorrect)
        is NotEnoughPartialsException -> context.getString(R.string.Not_enough_partials)
        is NotAllSignerSuccessException -> if (allFailure()) {
            context.getString(R.string.All_signer_failure)
        } else {
            reportException(this)
            context.getString(R.string.Not_all_signer_success) + this.getStackTraceString()
        }
        is DifferentIdentityException -> context.getString(R.string.PIN_not_same_as_last_time)
        else -> "${context.getString(R.string.Set_or_update_PIN_failed)}\n$localizedMessage"
    }
