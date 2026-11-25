package one.mixin.android.ui.transfer

import kotlinx.serialization.ExperimentalSerializationApi
import one.mixin.android.ui.transfer.status.TransferStatus
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransferClientFinalWorkTest {
    private val accountId = UUID.randomUUID().toString()
    private val peerDeviceId = "peer-device-1"

    private lateinit var manager: TransferSessionManager
    private lateinit var status: TransferStatusLiveData
    private lateinit var sessionId: String

    @Before
    fun setUp() {
        manager = TransferSessionManager()
        val session = manager.initiateSession(TransferDirection.RESTORE_FROM_PHONE, accountId)
        manager.beginPairingConfirmation(session.sessionId, accountId, peerDeviceId, 123456)
        manager.confirmAndConsumePairing(session.sessionId, accountId, peerDeviceId)
        sessionId = session.sessionId
        status = TransferStatusLiveData().apply { value = TransferStatus.PROCESSING }
    }

    @Test
    fun `attachment processing failure marks status as error instead of staying in processing`() {
        var cleaned = false

        TransferClient.failAttachmentProcessing(status, manager, sessionId, cancelledByUser = false) { cleaned = true }

        assertEquals(TransferStatus.ERROR, status.value)
        assertEquals(true, cleaned)
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(sessionId))
    }

    @Test
    fun `user cancellation does not report an error`() {
        manager.cancelSession(sessionId)

        TransferClient.failAttachmentProcessing(status, manager, sessionId, cancelledByUser = true) {}

        assertEquals(TransferStatus.PROCESSING, status.value)
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(sessionId))
    }
}
