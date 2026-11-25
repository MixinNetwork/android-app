package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.api.ChecksumException
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.ExpiredTokenException
import one.mixin.android.api.LocalJobException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.WebSocketException
import one.mixin.android.session.MissingAccountScopeException
import one.mixin.android.util.ErrorHandler.Companion.CONVERSATION_CHECKSUM_INVALID_ERROR
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.OLD_VERSION
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.CREATE_MESSAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class OldVersionJobRetryTest {
    private class TestBaseJob : BaseJob(Params(PRIORITY_SEND_MESSAGE)) {
        override fun onRun() = Unit
    }

    private class TestMixinJob : MixinJob(Params(PRIORITY_SEND_MESSAGE), "test-job-id") {
        override fun onRun() = Unit

        override fun cancel() {
            isCancelled = true
        }
    }

    @Test
    fun baseJobShouldRetryReturnsFalseForClientErrorExceptionAndPreservesOtherIoExceptions() {
        val job = TestBaseJob()

        assertFalse(job.shouldRetry(ClientErrorException(OLD_VERSION)))
        assertFalse(job.shouldRetry(ClientErrorException(400)))
        assertFalse(job.shouldRetry(MissingAccountScopeException("test")))

        assertTrue(job.shouldRetry(IOException("io")))
        assertTrue(job.shouldRetry(SocketTimeoutException("timeout")))
        assertTrue(job.shouldRetry(InterruptedException("interrupted")))
        assertTrue(job.shouldRetry(ServerErrorException(500)))
        assertTrue(job.shouldRetry(ExpiredTokenException()))
        assertTrue(job.shouldRetry(NetworkException()))
        assertTrue(job.shouldRetry(WebSocketException()))
        assertTrue(job.shouldRetry(ChecksumException()))
        assertTrue(job.shouldRetry(DataErrorException()))
        assertTrue(job.shouldRetry(LocalJobException()))
    }

    @Test
    fun mixinJobShouldRetryKeepsPendingJobAliveUnlessCancelled() {
        val job = TestMixinJob()
        assertTrue(job.shouldRetry(WebSocketException()))

        job.cancel()
        assertFalse(job.shouldRetry(WebSocketException()))
    }

    @Test
    fun handleDeliverResponseThrowsRetryableWebSocketExceptionWithoutSleepingOnOldVersion() {
        val outgoing = BlazeMessage(id = "msg-1", action = CREATE_MESSAGE, params = BlazeMessageParam(conversation_id = "conv-1"))
        val response =
            BlazeMessage(
                id = "msg-1",
                action = CREATE_MESSAGE,
                params = null,
                error = ResponseError(403, OLD_VERSION, "old version"),
            )
        var sleepCount = 0
        var syncedConversation: String? = null

        try {
            handleDeliverResponse(
                blazeMessage = outgoing,
                bm = response,
                onChecksumInvalid = { syncedConversation = it },
                onSleep = { sleepCount += 1 },
            )
            fail("Expected WebSocketException")
        } catch (e: WebSocketException) {
            assertTrue(e.shouldRetry())
        }

        assertEquals(0, sleepCount)
        assertEquals(null, syncedConversation)
    }

    @Test
    fun handleDeliverResponsePreservesOtherBranches() {
        val outgoing = BlazeMessage(id = "msg-2", action = CREATE_MESSAGE, params = BlazeMessageParam(conversation_id = "conv-2"))

        assertTrue(
            handleDeliverResponse(
                blazeMessage = outgoing,
                bm = BlazeMessage(id = "msg-2", action = CREATE_MESSAGE, params = null),
                onChecksumInvalid = {},
                onSleep = { fail("Should not sleep on success") },
            ),
        )

        assertTrue(
            handleDeliverResponse(
                blazeMessage = outgoing,
                bm = BlazeMessage(id = "msg-2", action = CREATE_MESSAGE, params = null, error = ResponseError(403, FORBIDDEN, "forbidden")),
                onChecksumInvalid = {},
                onSleep = { fail("Should not sleep on forbidden") },
            ),
        )

        var synced: String? = null
        try {
            handleDeliverResponse(
                blazeMessage = outgoing,
                bm = BlazeMessage(
                    id = "msg-2",
                    action = CREATE_MESSAGE,
                    params = null,
                    error = ResponseError(400, CONVERSATION_CHECKSUM_INVALID_ERROR, "checksum"),
                ),
                onChecksumInvalid = { synced = it },
                onSleep = { fail("Should not sleep on checksum error") },
            )
            fail("Expected ChecksumException")
        } catch (_: ChecksumException) {
            assertEquals("conv-2", synced)
        }

        var sleepCount = 0
        try {
            handleDeliverResponse(
                blazeMessage = outgoing,
                bm = null,
                onChecksumInvalid = {},
                onSleep = { sleepCount += 1 },
            )
            fail("Expected WebSocketException")
        } catch (_: WebSocketException) {
            assertEquals(1, sleepCount)
        }
    }
}
