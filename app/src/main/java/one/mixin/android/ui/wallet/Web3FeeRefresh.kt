package one.mixin.android.ui.wallet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import one.mixin.android.extension.numberFormat8
import timber.log.Timber
import java.math.BigDecimal

internal fun nativeWeb3FeeText(fee: BigDecimal?, symbol: String): String =
    fee?.let { "${it.numberFormat8()} $symbol" }.orEmpty()

internal suspend fun refreshWeb3TransferFees(
    retryDelayMillis: Long = 3000L,
    refreshNativeFee: suspend () -> Boolean,
    refreshGaslessFee: suspend () -> Boolean,
    onRefresh: () -> Unit,
) {
    suspend fun attempt(refresh: suspend () -> Boolean): Boolean {
        currentCoroutineContext().ensureActive()
        return try {
            refresh().also { currentCoroutineContext().ensureActive() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error)
            false
        }
    }

    var nativeReady = false
    var gaslessReady = false
    while (!nativeReady || !gaslessReady) {
        if (!nativeReady) nativeReady = attempt(refreshNativeFee)
        if (!gaslessReady) gaslessReady = attempt(refreshGaslessFee)
        onRefresh()
        if (!nativeReady || !gaslessReady) delay(retryDelayMillis)
    }
}
