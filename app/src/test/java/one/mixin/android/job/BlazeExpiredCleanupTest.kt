package one.mixin.android.job

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BlazeExpiredCleanupTest {
    @Test
    fun wakeupsAreCoalesced() = runBlocking {
        val wakeup = ExpiredCleanupWakeup()
        repeat(1000) { wakeup.signal() }
        withTimeout(1000) { wakeup.await(null) }
        assertNull(withTimeoutOrNull(20) { wakeup.await(null); true })
    }

    @Test
    fun onlyEarlierDeadlineWakesScheduledWait() = runBlocking {
        val wakeup = ExpiredCleanupWakeup()
        val deadline = System.currentTimeMillis() / 1000 + 3600
        val waiter = launch(start = CoroutineStart.UNDISPATCHED) { wakeup.await(deadline) }
        repeat(1000) { wakeup.signal(deadline + 1) }
        yield()
        assertFalse(waiter.isCompleted)
        wakeup.signal(deadline - 1)
        withTimeout(1000) { waiter.join() }
        assertFalse(waiter.isCancelled)
    }

    @Test
    fun dueDeadlineDoesNotWaitForAnotherEvent() = runBlocking {
        withTimeout(1000) {
            ExpiredCleanupWakeup().await(System.currentTimeMillis() / 1000 - 1)
        }
    }

    @Test
    fun signalBeforeWaitIsNotLost() = runBlocking {
        val wakeup = ExpiredCleanupWakeup()
        wakeup.signal(System.currentTimeMillis() / 1000)
        withTimeout(1000) {
            wakeup.await(System.currentTimeMillis() / 1000 + 3600)
        }
    }

    @Test
    fun transientFailureRetriesCleanup() =
        runBlocking {
            var attempts = 0

            runExpiredCleanupWithRetry(
                retryDelayMillis = 0,
                onFailure = {},
            ) {
                attempts++
                if (attempts == 1) {
                    throw IllegalStateException("transient")
                }
            }

            assertEquals(2, attempts)
        }

    @Test
    fun cancellationIsNotRetried() {
        var attempts = 0

        assertThrows(CancellationException::class.java) {
            runBlocking {
                runExpiredCleanupWithRetry(
                    retryDelayMillis = 0,
                    onFailure = {},
                ) {
                    attempts++
                    throw CancellationException()
                }
            }
        }

        assertEquals(1, attempts)
    }
}
