package one.mixin.android.tip.wc.internal

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal class WalletConnectPairingErrors {
    private val errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)

    fun report(error: Throwable) {
        errors.tryEmit(error)
    }

    suspend fun await(timeoutMillis: Long = 30_000): Throwable? =
        withTimeoutOrNull(timeoutMillis) { errors.first() }
}
