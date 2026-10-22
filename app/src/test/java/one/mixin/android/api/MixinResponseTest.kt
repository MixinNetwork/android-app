package one.mixin.android.api

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MixinResponseTest {
    @Test
    fun cancellingRequestDoesNotRunUiCallbacks() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val callbacks = mutableListOf<String>()
        val request = launch {
            handleMixinResponse<Unit, Unit>(
                invokeNetwork = {
                    started.complete(Unit)
                    awaitCancellation()
                },
                successBlock = { callbacks.add("success") },
                failureBlock = { callbacks.add("failure") },
                exceptionBlock = { callbacks.add("exception") },
                doAfterNetworkSuccess = { callbacks.add("network") },
                defaultErrorHandle = { callbacks.add("defaultError") },
                defaultExceptionHandle = { callbacks.add("defaultException") },
                endBlock = { callbacks.add("end") },
            )
        }

        started.await()
        request.cancelAndJoin()

        assertTrue(request.isCancelled)
        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun transportFailureStillRunsErrorHandlingAndCleanup() = runBlocking {
        val failure = IOException("connection failed")
        val callbacks = mutableListOf<String>()
        val result = handleMixinResponse<Unit, Unit>(
            invokeNetwork = { throw failure },
            switchContext = Dispatchers.Unconfined,
            exceptionBlock = {
                assertSame(failure, it)
                callbacks.add("exception")
                false
            },
            defaultExceptionHandle = {
                assertSame(failure, it)
                callbacks.add("defaultException")
            },
            endBlock = { callbacks.add("end") },
        )

        assertNull(result)
        assertEquals(listOf("exception", "defaultException", "end"), callbacks)
    }

    @Test
    fun handledApiFailureSkipsDefaultErrorAndRunsCleanup() = runBlocking {
        val callbacks = mutableListOf<String>()
        val result = handleMixinResponse<Unit, Unit>(
            invokeNetwork = { MixinResponse(ResponseError(404, 404, "not found")) },
            switchContext = Dispatchers.Unconfined,
            successBlock = { callbacks.add("success") },
            failureBlock = {
                assertEquals(404, it.errorCode)
                callbacks.add("failure")
                true
            },
            defaultErrorHandle = { callbacks.add("defaultError") },
            endBlock = { callbacks.add("end") },
        )

        assertNull(result)
        assertEquals(listOf("failure", "end"), callbacks)
    }
}
