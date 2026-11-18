package one.mixin.android.ui.transfer

import one.mixin.android.ui.transfer.vo.TransferCommandAction
import org.junit.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransferSessionManagerTest {
    private val accountId = UUID.randomUUID().toString()
    private val otherAccountId = UUID.randomUUID().toString()
    private val extensionSessionId = UUID.randomUUID().toString()
    private val otherExtensionSessionId = UUID.randomUUID().toString()
    private val peerDeviceId = "peer-device-1"
    private val otherPeerDeviceId = "peer-device-2"

    @Test
    fun `remote control requires matching account, extension session, and non-expired timestamp`() {
        var now = 1_700_000_000_000L
        val manager = TransferSessionManager { now }
        val validTimestamp = Instant.ofEpochMilli(now - 30_000L).toString()
        val expiredTimestamp = Instant.ofEpochMilli(now - TransferSessionManager.DEFAULT_MESSAGE_MAX_AGE_MS - 1_000L).toString()

        assertTrue(
            manager.isRemoteControlAuthorized(
                senderUserId = accountId,
                senderSessionId = extensionSessionId,
                messageCreatedAt = validTimestamp,
                expectedAccountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ),
        )
        assertFalse(
            manager.isRemoteControlAuthorized(
                senderUserId = otherAccountId,
                senderSessionId = extensionSessionId,
                messageCreatedAt = validTimestamp,
                expectedAccountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ),
        )
        assertFalse(
            manager.isRemoteControlAuthorized(
                senderUserId = accountId,
                senderSessionId = otherExtensionSessionId,
                messageCreatedAt = validTimestamp,
                expectedAccountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ),
        )
        assertFalse(
            manager.isRemoteControlAuthorized(
                senderUserId = accountId,
                senderSessionId = extensionSessionId,
                messageCreatedAt = expiredTimestamp,
                expectedAccountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ),
        )
        assertFalse(
            manager.isRemoteControlAuthorized(
                senderUserId = accountId,
                senderSessionId = extensionSessionId,
                messageCreatedAt = "not-a-timestamp",
                expectedAccountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ),
        )
    }

    @Test
    fun `uninitiated remote commands are rejected even for matching account and device`() {
        val manager = TransferSessionManager { 1_700_000_000_000L }

        assertFalse(
            manager.canAcceptRemoteCommand(
                action = TransferCommandAction.PUSH.value,
                senderUserId = accountId,
                senderSessionId = extensionSessionId,
                commandUserId = accountId,
            ),
        )
    }

    @Test
    fun `one-time pairing confirmation binds device and rejects replay, expiry, and cancellation`() {
        var now = 1_700_000_000_000L
        val manager = TransferSessionManager { now }

        val session = manager.initiateSession(TransferDirection.RESTORE_FROM_PHONE, accountId)
        assertTrue(manager.beginPairingConfirmation(session.sessionId, accountId, peerDeviceId, 123456))

        // Device mismatch on confirmation is rejected
        assertNull(manager.confirmAndConsumePairing(session.sessionId, accountId, otherPeerDeviceId))
        // Account mismatch on confirmation is rejected
        assertNull(manager.confirmAndConsumePairing(session.sessionId, otherAccountId, peerDeviceId))

        // First confirmation succeeds and consumes the one-time session
        val auth = manager.confirmAndConsumePairing(session.sessionId, accountId, peerDeviceId)
        assertNotNull(auth)
        assertEquals(TransferSessionState.CONSUMED, manager.getSessionState(session.sessionId))

        // Replaying the same one-time session is rejected
        assertNull(manager.confirmAndConsumePairing(session.sessionId, accountId, peerDeviceId))

        // Handshake checks account and bound peer device
        assertFalse(manager.validateStartHandshake(auth, otherAccountId, accountId, peerDeviceId))

        // Re-initiate and verify handshake rejects peer device mismatch and revokes session
        val session2 = manager.initiateSession(TransferDirection.RESTORE_FROM_PHONE, accountId)
        assertTrue(manager.beginPairingConfirmation(session2.sessionId, accountId, peerDeviceId, 654321))
        val auth2 = manager.confirmAndConsumePairing(session2.sessionId, accountId, peerDeviceId)
        assertNotNull(auth2)
        assertFalse(manager.validateStartHandshake(auth2, accountId, accountId, otherPeerDeviceId))
        assertFalse(manager.isAuthorizationActiveForWrite(auth2, accountId))
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(session2.sessionId))
    }

    @Test
    fun `expired and cancelled sessions cannot be paired or written`() {
        var now = 1_700_000_000_000L
        val manager = TransferSessionManager { now }

        val expiringSession =
            manager.initiateSession(
                direction = TransferDirection.RESTORE_FROM_PC,
                accountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
                ttlMs = 60_000L,
            )
        assertTrue(
            manager.canAcceptRemoteCommand(
                action = TransferCommandAction.PUSH.value,
                senderUserId = accountId,
                senderSessionId = extensionSessionId,
                commandUserId = accountId,
            ),
        )

        // Advance clock past TTL
        now += 61_000L
        assertFalse(
            manager.canAcceptRemoteCommand(
                action = TransferCommandAction.PUSH.value,
                senderUserId = accountId,
                senderSessionId = extensionSessionId,
                commandUserId = accountId,
            ),
        )
        assertEquals(TransferSessionState.EXPIRED, manager.getSessionState(expiringSession.sessionId))
        assertFalse(manager.beginPairingConfirmation(expiringSession.sessionId, accountId, peerDeviceId, 1111))

        // Active transfer aborted on CANCEL stops further writes
        val activeSession =
            manager.initiateSession(
                direction = TransferDirection.RESTORE_FROM_PC,
                accountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            )
        assertTrue(manager.beginPairingConfirmation(activeSession.sessionId, accountId, null, 2222))
        val auth = manager.confirmAndConsumePairing(activeSession.sessionId, accountId, null)
        assertNotNull(auth)
        assertTrue(manager.validateStartHandshake(auth, accountId, accountId, peerDeviceId))
        assertTrue(manager.isAuthorizationActiveForWrite(auth, accountId))

        manager.cancelActiveSessionIfMatches(accountId, extensionSessionId)
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(activeSession.sessionId))
        assertFalse(manager.isAuthorizationActiveForWrite(auth, accountId))
    }
}
