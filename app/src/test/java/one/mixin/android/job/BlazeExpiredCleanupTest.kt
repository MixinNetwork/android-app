package one.mixin.android.job

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BlazeExpiredCleanupTest {
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
