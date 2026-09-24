package one.mixin.android.ui.wallet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Web3FeeRefreshTest {
    @Test
    fun unknownFeeIsBlankAndZeroIsStillDisplayed() {
        assertEquals("", nativeWeb3FeeText(null, "SOL"))
        assertEquals("0 SOL", nativeWeb3FeeText(BigDecimal.ZERO, "SOL"))
        assertEquals("1 ETH", nativeWeb3FeeText(BigDecimal.ONE, "ETH"))
    }

    @Test
    fun errorsAndUnsuccessfulResponsesRetryEachFeeIndependently() = runBlocking {
        var nativeAttempts = 0
        var gaslessAttempts = 0
        val refreshes = mutableListOf<Pair<Int, Int>>()

        refreshWeb3TransferFees(
            retryDelayMillis = 1,
            refreshNativeFee = {
                nativeAttempts++
                if (nativeAttempts == 1) throw IOException("Native fee unavailable")
                true
            },
            refreshGaslessFee = {
                gaslessAttempts++
                when (gaslessAttempts) {
                    1 -> false
                    2 -> throw IOException("Gasless fee unavailable")
                    else -> true
                }
            },
            onRefresh = { refreshes.add(nativeAttempts to gaslessAttempts) },
        )

        assertEquals(listOf(1 to 1, 2 to 2, 2 to 3), refreshes)
    }

    @Test
    fun cancellationIsNotRetried() = runBlocking {
        var nativeAttempts = 0
        var gaslessAttempts = 0
        assertFailsWith<CancellationException> {
            refreshWeb3TransferFees(
                retryDelayMillis = 1,
                refreshNativeFee = {
                    nativeAttempts++
                    throw CancellationException()
                },
                refreshGaslessFee = { gaslessAttempts++; true },
                onRefresh = { error("Cancelled refresh must not update the UI") },
            )
        }
        assertEquals(1, nativeAttempts)
        assertEquals(0, gaslessAttempts)
    }

    @Test
    fun retryWaitIsCancellableAndDoesNotImmediatelyRepeatRequests() = runBlocking {
        var attempts = 0
        val refresh = launch(start = CoroutineStart.UNDISPATCHED) {
            refreshWeb3TransferFees(
                retryDelayMillis = 10_000,
                refreshNativeFee = { attempts++; false },
                refreshGaslessFee = { attempts++; false },
                onRefresh = {},
            )
        }

        assertEquals(2, attempts)
        refresh.cancelAndJoin()
        assertEquals(2, attempts)
    }
}
