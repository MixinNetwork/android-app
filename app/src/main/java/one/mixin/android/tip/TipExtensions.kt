package one.mixin.android.tip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.TipSigner
import one.mixin.android.util.reportException
import timber.log.Timber
import java.io.IOException

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
            context.getString(R.string.Not_all_signer_success)
        }
        is DifferentIdentityException -> context.getString(R.string.PIN_not_same_as_last_time)
        else -> "${context.getString(R.string.Set_or_update_PIN_failed)}\n$localizedMessage"
    }

@Throws(IOException::class, TipNullException::class)
suspend fun Tip.checkCounter(
    tipCounter: Int,
    onNodeCounterGreaterThanServer: suspend (Int) -> Unit,
    onNodeCounterInconsistency: suspend (Int, List<TipSigner>?) -> Unit,
) {
    val counters = watchTipNodeCounters()
    if (counters.isEmpty()) {
        Timber.w("watch tip node counters but counters is empty")
        throw TipNullException("watch tip node counters but counters is empty")
    }

    if (counters.size != tipNodeCount()) {
        Timber.w("watch tip node result size is ${counters.size} is not equals to node count ${tipNodeCount()}")
        // TODO should we consider this case as an incomplete state?
    }
    val group = counters.groupBy { it.counter }
    if (group.size <= 1) {
        val nodeCounter = counters.first().counter
        Timber.e("watch tip node all counter are $nodeCounter, tipCounter $tipCounter")
        if (nodeCounter == tipCounter) {
            return
        }
        if (nodeCounter < tipCounter) {
            reportIllegal("watch tip node node counter $nodeCounter < tipCounter $tipCounter")
            return
        }

        onNodeCounterGreaterThanServer(nodeCounter)
        return
    }
    if (group.size > 2) {
        reportIllegal("watch tip node meet ${group.size} kinds of counter!")
        return
    }

    val maxCounter = group.keys.maxBy { it }
    val failedNodes = group[group.keys.minBy { it }]
    val failedSigners = if (failedNodes != null) {
        val signers = mutableListOf<TipSigner>()
        failedNodes.mapTo(signers) { it.tipSigner }
    } else null
    Timber.e("watch tip node counter maxCounter $maxCounter, need update nodes: $failedSigners")
    onNodeCounterInconsistency(maxCounter, failedSigners)
}

private fun reportIllegal(msg: String) {
    Timber.w(msg)
    reportException(IllegalStateException(msg))
}
