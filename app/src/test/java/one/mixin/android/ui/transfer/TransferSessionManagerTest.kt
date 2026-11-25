package one.mixin.android.ui.transfer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import one.mixin.android.extension.base64Encode
import one.mixin.android.ui.transfer.vo.TransferCommand
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
    fun `START without a device ID retains the peer confirmed in PUSH`() {
        val manager = TransferSessionManager { 1_700_000_000_000L }
        val session = manager.initiateSession(TransferDirection.RESTORE_FROM_PC, accountId, extensionSessionId)
        val push = TransferCommand(TransferCommandAction.PUSH.value, deviceId = peerDeviceId)
        assertTrue(manager.beginPairingConfirmation(session.sessionId, accountId, push.deviceId, 123456))
        val auth = assertNotNull(manager.confirmAndConsumePairing(session.sessionId, accountId, push.deviceId))

        val start = Json.decodeFromString<TransferCommand>("""{"action":"start"}""")
        assertNull(start.deviceId)
        assertTrue(manager.validateStartHandshake(auth, accountId, accountId, start.deviceId))
        assertEquals(peerDeviceId, auth.expectedPeerDeviceId)
        val outgoing = start.copy(deviceId = peerDeviceId)
        assertTrue(Json.parseToJsonElement(Json.encodeToString(outgoing)).jsonObject.containsKey("device_id"))
        assertEquals(peerDeviceId, Json.decodeFromString<TransferCommand>(Json.encodeToString(outgoing)).deviceId)
        assertFalse(manager.validateStartHandshake(auth, accountId, accountId, otherPeerDeviceId))
        assertFalse(manager.isAuthorizationActiveForWrite(auth, accountId))
    }

    @Test
    fun `desktop cancellation only revokes a matching desktop session`() {
        val manager = TransferSessionManager { 1_700_000_000_000L }
        for (direction in listOf(TransferDirection.RESTORE_FROM_PHONE, TransferDirection.TRANSFER_TO_PHONE)) {
            val session = manager.initiateSession(direction, accountId)
            assertFalse(manager.cancelActiveSessionIfMatches(accountId, extensionSessionId))
            assertFalse(manager.cancelActiveSessionIfMatches(accountId, null))
            assertEquals(TransferSessionState.INITIATED, manager.getSessionState(session.sessionId))
        }
        val session = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertTrue(manager.beginSending(session.sessionId, accountId, extensionSessionId))
        assertFalse(manager.cancelActiveSessionIfMatches(otherAccountId, extensionSessionId))
        assertFalse(manager.cancelActiveSessionIfMatches(accountId, otherExtensionSessionId))
        assertEquals(TransferSessionState.SENDING, manager.getSessionState(session.sessionId))
        assertTrue(manager.cancelActiveSessionIfMatches(accountId, extensionSessionId))
        assertFalse(manager.completeSession(session.sessionId))
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(session.sessionId))
    }

    @Test
    fun `rejecting a desktop pull allows a fresh approval on the same screen`() {
        val manager = TransferSessionManager { 1_700_000_000_000L }
        val rejected = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertTrue(manager.beginPairingConfirmation(rejected.sessionId, accountId, peerDeviceId, null))
        manager.cancelSession(rejected.sessionId)
        val next = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertNull(manager.confirmAndConsumePairing(rejected.sessionId, accountId, peerDeviceId))
        assertTrue(manager.canAcceptRemoteCommand(TransferCommandAction.PULL.value, accountId, extensionSessionId, accountId))
        assertTrue(manager.beginPairingConfirmation(next.sessionId, accountId, peerDeviceId, null))
        assertNotNull(manager.confirmAndConsumePairing(next.sessionId, accountId, peerDeviceId))
        assertTrue(manager.beginSending(next.sessionId, accountId, extensionSessionId))
    }

    @Test
    fun `sending authorization expires during transfer and is isolated between attempts`() {
        var now = 1_700_000_000_000L
        val manager = TransferSessionManager { now }
        val first = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId, ttlMs = 1000L)
        assertFalse(manager.beginSending(first.sessionId, otherAccountId, extensionSessionId))
        assertTrue(manager.beginSending(first.sessionId, accountId, extensionSessionId))
        assertFalse(manager.beginSending(first.sessionId, accountId, extensionSessionId))
        now += 1001L
        assertEquals(TransferSessionState.EXPIRED, manager.getSessionState(first.sessionId))
        assertFalse(manager.completeSession(first.sessionId))

        val second = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertNull(manager.getSessionState(first.sessionId))
        manager.cancelSession(first.sessionId)
        assertTrue(manager.canAcceptRemoteCommand(TransferCommandAction.PULL.value, accountId, extensionSessionId, accountId))
        assertTrue(manager.beginPairingConfirmation(second.sessionId, accountId, peerDeviceId, null))
        assertFalse(manager.beginSending(second.sessionId, accountId, extensionSessionId))
        assertNotNull(manager.confirmAndConsumePairing(second.sessionId, accountId, peerDeviceId))
        assertTrue(manager.beginSending(second.sessionId, accountId, extensionSessionId))
        assertTrue(manager.completeSession(second.sessionId))
        assertFalse(manager.cancelActiveSessionIfMatches(accountId, extensionSessionId))
    }

    @Test
    fun `verified sending session outlives pairing ttl and uses bounded transfer ttl`() {
        var now = 1_700_000_000_000L
        val manager = TransferSessionManager { now }

        for (direction in listOf(TransferDirection.TRANSFER_TO_PHONE, TransferDirection.TRANSFER_TO_PC)) {
            val extSessionId = if (direction == TransferDirection.TRANSFER_TO_PC) extensionSessionId else null
            val session = manager.initiateSession(direction, accountId, extSessionId, ttlMs = 5_000L, transferTtlMs = 20_000L)
            assertTrue(manager.beginSending(session.sessionId, accountId, extSessionId))

            now += 4_500L
            assertTrue(
                manager.validateServerConnect(
                    sessionId = session.sessionId,
                    currentAccountId = accountId,
                    currentExtensionSessionId = extSessionId,
                    remoteUserId = accountId,
                    remoteDeviceId = peerDeviceId,
                    receivedCode = 4242,
                    expectedCode = 4242,
                ),
            )

            // Past original pairing TTL (4500 + 1000 > 5000) still active during transfer
            now += 1_000L
            assertEquals(TransferSessionState.SENDING, manager.getSessionState(session.sessionId))

            // Still active just before transfer TTL expires
            now += 18_500L
            assertEquals(TransferSessionState.SENDING, manager.getSessionState(session.sessionId))

            // Expires once bounded transfer TTL elapses
            now += 501L
            assertEquals(TransferSessionState.EXPIRED, manager.getSessionState(session.sessionId))
            assertFalse(manager.completeSession(session.sessionId))
        }
    }

    @Test
    fun `late CONNECT after long preparation still passes within a fresh connect window`() {
        var now = 1_700_000_000_000L
        val manager = TransferSessionManager { now }
        val session = manager.initiateSession(TransferDirection.TRANSFER_TO_PHONE, accountId)
        // Preparation consumes almost the whole pairing TTL
        now += TransferSessionManager.DEFAULT_PAIRING_TTL_MS - 1_000L
        assertTrue(manager.beginSending(session.sessionId, accountId))

        now += 10_000L
        assertTrue(
            manager.validateServerConnect(
                sessionId = session.sessionId,
                currentAccountId = accountId,
                currentExtensionSessionId = null,
                remoteUserId = accountId,
                remoteDeviceId = peerDeviceId,
                receivedCode = 4242,
                expectedCode = 4242,
            ),
        )
        assertEquals(TransferSessionState.SENDING, manager.getSessionState(session.sessionId))

        val unconnected = manager.initiateSession(TransferDirection.TRANSFER_TO_PHONE, accountId)
        assertTrue(manager.beginSending(unconnected.sessionId, accountId))
        now += TransferSessionManager.DEFAULT_PAIRING_TTL_MS + 1L
        assertEquals(TransferSessionState.EXPIRED, manager.getSessionState(unconnected.sessionId))
    }

    @Test
    fun `beginSending and validateServerConnect reject desktop relink, wrong code, and peer mismatch`() {
        val manager = TransferSessionManager { 1_700_000_000_000L }

        val relinkedBeforeStart = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertEquals(extensionSessionId, manager.getBoundExtensionSessionId(relinkedBeforeStart.sessionId))
        assertFalse(manager.beginSending(relinkedBeforeStart.sessionId, accountId, otherExtensionSessionId))
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(relinkedBeforeStart.sessionId))
        // Bound extension session stays accessible for cancellation even after session is cancelled
        assertEquals(extensionSessionId, manager.getBoundExtensionSessionId(relinkedBeforeStart.sessionId))

        val relinkedBeforeConnect = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertTrue(manager.beginPairingConfirmation(relinkedBeforeConnect.sessionId, accountId, peerDeviceId, null))
        assertNotNull(manager.confirmAndConsumePairing(relinkedBeforeConnect.sessionId, accountId, peerDeviceId))
        assertTrue(manager.beginSending(relinkedBeforeConnect.sessionId, accountId, extensionSessionId))
        assertFalse(
            manager.validateServerConnect(
                sessionId = relinkedBeforeConnect.sessionId,
                currentAccountId = accountId,
                currentExtensionSessionId = otherExtensionSessionId,
                remoteUserId = accountId,
                remoteDeviceId = peerDeviceId,
                receivedCode = 1111,
                expectedCode = 1111,
            ),
        )
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(relinkedBeforeConnect.sessionId))

        val wrongPeer = manager.initiateSession(TransferDirection.TRANSFER_TO_PC, accountId, extensionSessionId)
        assertTrue(manager.beginPairingConfirmation(wrongPeer.sessionId, accountId, peerDeviceId, null))
        assertNotNull(manager.confirmAndConsumePairing(wrongPeer.sessionId, accountId, peerDeviceId))
        assertTrue(manager.beginSending(wrongPeer.sessionId, accountId, extensionSessionId))
        assertFalse(
            manager.validateServerConnect(
                sessionId = wrongPeer.sessionId,
                currentAccountId = accountId,
                currentExtensionSessionId = extensionSessionId,
                remoteUserId = accountId,
                remoteDeviceId = otherPeerDeviceId,
                receivedCode = 1111,
                expectedCode = 1111,
            ),
        )
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(wrongPeer.sessionId))
    }

    @Test
    fun `decodeTransferSecretKey accepts 64-byte base64 and rejects malformed or short keys`() {
        val validKey = ByteArray(TRANSFER_SECRET_KEY_BYTES) { it.toByte() }
        val decoded = decodeTransferSecretKey(validKey.base64Encode())
        assertNotNull(decoded)
        assertTrue(validKey.contentEquals(decoded))

        assertNull(decodeTransferSecretKey(null))
        assertNull(decodeTransferSecretKey(""))
        assertNull(decodeTransferSecretKey("   "))
        assertNull(decodeTransferSecretKey("not-valid-base64!!!"))
        assertNull(decodeTransferSecretKey(ByteArray(32) { it.toByte() }.base64Encode()))
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
