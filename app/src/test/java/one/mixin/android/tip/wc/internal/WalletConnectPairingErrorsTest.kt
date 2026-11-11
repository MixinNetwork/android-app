package one.mixin.android.tip.wc.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WalletConnectPairingErrorsTest {
    @Test
    fun sdkErrorEndsActivePairingWait() = runBlocking {
        val errors = WalletConnectPairingErrors()
        val pending = async(start = CoroutineStart.UNDISPATCHED) { errors.await() }
        val error = IllegalStateException("No pending proposal")

        errors.report(error)

        assertSame(error, pending.await())
    }

    @Test
    fun missingProposalTimesOut() = runBlocking {
        assertNull(WalletConnectPairingErrors().await(timeoutMillis = 10))
    }

    @Test
    fun backgroundErrorsAreNotReplayedWhenPairingStarts() = runBlocking {
        val errors = WalletConnectPairingErrors()
        errors.report(IllegalStateException("Previous session error"))

        assertNull(errors.await(timeoutMillis = 10))
    }

    @Test
    fun dismissingConnectionCancelsWaitWithoutAffectingRetry() = runBlocking {
        val errors = WalletConnectPairingErrors()
        val dismissed = async(start = CoroutineStart.UNDISPATCHED) { errors.await() }
        dismissed.cancelAndJoin()
        assertTrue(dismissed.isCancelled)
        errors.report(IllegalStateException("Error while dismissed"))

        val retry = async(start = CoroutineStart.UNDISPATCHED) { errors.await() }
        val error = IllegalStateException("Current pairing error")
        errors.report(error)

        assertSame(error, retry.await())
    }
}
