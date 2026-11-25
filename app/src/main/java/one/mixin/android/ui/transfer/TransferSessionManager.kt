package one.mixin.android.ui.transfer

import one.mixin.android.ui.transfer.vo.TransferCommand
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TransferDirection {
    RESTORE_FROM_PHONE,
    RESTORE_FROM_PC,
    TRANSFER_TO_PHONE,
    TRANSFER_TO_PC;

    val isSending: Boolean get() = this == TRANSFER_TO_PHONE || this == TRANSFER_TO_PC
}

enum class TransferSessionState {
    INITIATED,
    AWAITING_PAIRING,
    CONSUMED,
    SENDING,
    CANCELLED,
    EXPIRED,
    COMPLETED,
}

data class TransferSession(
    val sessionId: String,
    val direction: TransferDirection,
    val accountId: String,
    val expectedExtensionSessionId: String?,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val transferTtlMs: Long = TransferSessionManager.DEFAULT_TRANSFER_TTL_MS,
    var transferExpiresAtMillis: Long? = null,
    var boundPeerDeviceId: String? = null,
    var boundPairingCode: Int? = null,
    var state: TransferSessionState = TransferSessionState.INITIATED,
)

data class TransferAuthorization(
    val sessionId: String,
    val direction: TransferDirection,
    val expectedAccountId: String,
    val expectedExtensionSessionId: String?,
    var expectedPeerDeviceId: String?,
    val pairingCode: Int?,
    val expiresAtMillis: Long,
)

data class IncomingTransferControlEvent(
    val command: TransferCommand,
    val senderUserId: String,
    val senderSessionId: String,
)

/**
 * Coordinates one-time, locally initiated device-transfer sessions and enforces pairing, account,
 * device, expiry, and cancellation checks before any transfer connection or write proceeds.
 */
@Singleton
class TransferSessionManager(
    private val clock: () -> Long,
) {
    @Inject
    constructor() : this(System::currentTimeMillis)

    companion object {
        const val DEFAULT_PAIRING_TTL_MS = 5 * 60 * 1000L
        const val DEFAULT_TRANSFER_TTL_MS = 2 * 60 * 60 * 1000L
        const val DEFAULT_MESSAGE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val FUTURE_CLOCK_SKEW_MS = 2 * 60 * 1000L
    }

    private var currentSession: TransferSession? = null

    @Synchronized
    fun initiateSession(
        direction: TransferDirection,
        accountId: String,
        expectedExtensionSessionId: String? = null,
        ttlMs: Long = DEFAULT_PAIRING_TTL_MS,
        transferTtlMs: Long = DEFAULT_TRANSFER_TTL_MS,
    ): TransferSession {
        require(accountId.isNotBlank()) { "Account ID must not be blank" }
        require(ttlMs > 0L) { "Session TTL must be positive" }
        require(transferTtlMs > 0L) { "Transfer TTL must be positive" }
        if (direction == TransferDirection.RESTORE_FROM_PC || direction == TransferDirection.TRANSFER_TO_PC) {
            require(!expectedExtensionSessionId.isNullOrBlank()) { "Desktop extension session ID is required" }
        }

        currentSession?.let { previous ->
            if (previous.state == TransferSessionState.INITIATED ||
                previous.state == TransferSessionState.AWAITING_PAIRING ||
                previous.state == TransferSessionState.CONSUMED || previous.state == TransferSessionState.SENDING
            ) {
                previous.state = TransferSessionState.CANCELLED
            }
        }

        val now = clock()
        val session =
            TransferSession(
                sessionId = UUID.randomUUID().toString(),
                direction = direction,
                accountId = accountId,
                expectedExtensionSessionId = expectedExtensionSessionId,
                createdAtMillis = now,
                expiresAtMillis = now + ttlMs,
                transferTtlMs = transferTtlMs,
            )
        currentSession = session
        return session
    }

    @Synchronized
    fun isRemoteControlAuthorized(
        senderUserId: String?,
        senderSessionId: String?,
        messageCreatedAt: String?,
        expectedAccountId: String?,
        expectedExtensionSessionId: String?,
    ): Boolean {
        if (expectedAccountId.isNullOrBlank() || expectedExtensionSessionId.isNullOrBlank()) {
            return false
        }
        if (senderUserId != expectedAccountId || senderSessionId != expectedExtensionSessionId) {
            return false
        }
        val createdAtMillis = parseIsoTimestampMillis(messageCreatedAt) ?: return false
        val now = clock()
        if (now - createdAtMillis > DEFAULT_MESSAGE_MAX_AGE_MS) {
            return false
        }
        if (createdAtMillis - now > FUTURE_CLOCK_SKEW_MS) {
            return false
        }
        return true
    }

    @Synchronized
    fun canAcceptRemoteCommand(
        action: String,
        senderUserId: String,
        senderSessionId: String,
        commandUserId: String?,
    ): Boolean {
        val session = currentSession ?: return false
        expireIfNeeded(session)
        if (session.state != TransferSessionState.INITIATED) {
            return false
        }
        if (session.accountId != senderUserId) {
            return false
        }
        if (!commandUserId.isNullOrBlank() && commandUserId != session.accountId) {
            return false
        }
        if (session.expectedExtensionSessionId.isNullOrBlank() || session.expectedExtensionSessionId != senderSessionId) {
            return false
        }
        return when (action) {
            TransferCommandAction.PUSH.value -> session.direction == TransferDirection.RESTORE_FROM_PC
            TransferCommandAction.PULL.value -> session.direction == TransferDirection.TRANSFER_TO_PC
            else -> false
        }
    }

    @Synchronized
    fun beginPairingConfirmation(
        sessionId: String?,
        accountId: String?,
        peerDeviceId: String?,
        pairingCode: Int?,
    ): Boolean {
        if (sessionId.isNullOrBlank() || accountId.isNullOrBlank()) return false
        val session = currentSession ?: return false
        if (session.sessionId != sessionId) return false
        expireIfNeeded(session)
        if (session.state != TransferSessionState.INITIATED) {
            return false
        }
        if (session.accountId != accountId) {
            return false
        }
        val normalizedPeerDeviceId = peerDeviceId?.takeIf { it.isNotBlank() }
        if (session.direction == TransferDirection.RESTORE_FROM_PHONE && normalizedPeerDeviceId == null) {
            return false
        }
        session.boundPeerDeviceId = normalizedPeerDeviceId
        session.boundPairingCode = pairingCode
        session.state = TransferSessionState.AWAITING_PAIRING
        return true
    }

    @Synchronized
    fun confirmAndConsumePairing(
        sessionId: String?,
        accountId: String?,
        peerDeviceId: String?,
    ): TransferAuthorization? {
        if (sessionId.isNullOrBlank() || accountId.isNullOrBlank()) return null
        val session = currentSession ?: return null
        if (session.sessionId != sessionId) return null
        expireIfNeeded(session)
        if (session.state != TransferSessionState.AWAITING_PAIRING) {
            return null
        }
        if (session.accountId != accountId) {
            return null
        }
        val normalizedPeerDeviceId = peerDeviceId?.takeIf { it.isNotBlank() }
        if (session.boundPeerDeviceId != null && session.boundPeerDeviceId != normalizedPeerDeviceId) {
            return null
        }
        session.state = TransferSessionState.CONSUMED
        return TransferAuthorization(
            sessionId = session.sessionId,
            direction = session.direction,
            expectedAccountId = session.accountId,
            expectedExtensionSessionId = session.expectedExtensionSessionId,
            expectedPeerDeviceId = session.boundPeerDeviceId,
            pairingCode = session.boundPairingCode,
            expiresAtMillis = session.expiresAtMillis,
        )
    }

    @Synchronized
    fun validateStartHandshake(
        authorization: TransferAuthorization?,
        currentAccountId: String?,
        remoteUserId: String?,
        remoteDeviceId: String?,
    ): Boolean {
        if (authorization == null || currentAccountId.isNullOrBlank()) {
            return false
        }
        val session = currentSession ?: return false
        if (session.sessionId != authorization.sessionId || session.state != TransferSessionState.CONSUMED) {
            return false
        }
        if (session.accountId != currentAccountId || authorization.expectedAccountId != currentAccountId) {
            session.state = TransferSessionState.CANCELLED
            return false
        }
        if (!remoteUserId.isNullOrBlank() && remoteUserId != authorization.expectedAccountId) {
            session.state = TransferSessionState.CANCELLED
            return false
        }
        val normalizedRemoteDeviceId = remoteDeviceId?.takeIf { it.isNotBlank() }
        val expectedPeer = authorization.expectedPeerDeviceId
        if (expectedPeer != null) {
            if (remoteDeviceId != null && normalizedRemoteDeviceId != expectedPeer) {
                session.state = TransferSessionState.CANCELLED
                return false
            }
        } else if (normalizedRemoteDeviceId != null) {
            authorization.expectedPeerDeviceId = normalizedRemoteDeviceId
            session.boundPeerDeviceId = normalizedRemoteDeviceId
        }
        return true
    }

    @Synchronized
    fun isAuthorizationActiveForWrite(
        authorization: TransferAuthorization?,
        currentAccountId: String?,
    ): Boolean {
        if (authorization == null || currentAccountId.isNullOrBlank()) {
            return false
        }
        val session = currentSession ?: return false
        if (session.sessionId != authorization.sessionId) {
            return false
        }
        if (session.state != TransferSessionState.CONSUMED) {
            return false
        }
        return session.accountId == currentAccountId && authorization.expectedAccountId == currentAccountId
    }

    @Synchronized
    fun getBoundExtensionSessionId(sessionId: String?): String? {
        val session = currentSession ?: return null
        if (sessionId != null && session.sessionId != sessionId) return null
        return session.expectedExtensionSessionId
    }

    @Synchronized
    fun beginSending(
        sessionId: String,
        accountId: String?,
        currentExtensionSessionId: String? = null,
    ): Boolean {
        val session = currentSession ?: return false
        expireIfNeeded(session)
        if (session.sessionId != sessionId || session.accountId != accountId ||
            !session.direction.isSending ||
            (session.state != TransferSessionState.INITIATED && session.state != TransferSessionState.CONSUMED)
        ) return false
        if (session.direction == TransferDirection.TRANSFER_TO_PC) {
            if (session.expectedExtensionSessionId.isNullOrBlank() ||
                session.expectedExtensionSessionId != currentExtensionSessionId
            ) {
                session.state = TransferSessionState.CANCELLED
                return false
            }
        }
        session.state = TransferSessionState.SENDING
        session.transferExpiresAtMillis = null
        return true
    }

    @Synchronized
    fun validateServerConnect(
        sessionId: String?,
        currentAccountId: String?,
        currentExtensionSessionId: String?,
        remoteUserId: String?,
        remoteDeviceId: String?,
        receivedCode: Int?,
        expectedCode: Int,
    ): Boolean {
        if (sessionId.isNullOrBlank() || currentAccountId.isNullOrBlank()) return false
        val session = currentSession ?: return false
        if (session.sessionId != sessionId) return false
        expireIfNeeded(session)
        if (session.state != TransferSessionState.SENDING) return false
        if (session.accountId != currentAccountId) {
            session.state = TransferSessionState.CANCELLED
            return false
        }
        if (session.direction == TransferDirection.TRANSFER_TO_PC) {
            if (session.expectedExtensionSessionId.isNullOrBlank() ||
                session.expectedExtensionSessionId != currentExtensionSessionId
            ) {
                session.state = TransferSessionState.CANCELLED
                return false
            }
        }
        if (receivedCode != expectedCode || remoteUserId != session.accountId) {
            session.state = TransferSessionState.CANCELLED
            return false
        }
        val normalizedRemoteDeviceId = remoteDeviceId?.takeIf { it.isNotBlank() }
        val expectedPeer = session.boundPeerDeviceId
        if (expectedPeer != null) {
            if (remoteDeviceId != null && normalizedRemoteDeviceId != expectedPeer) {
                session.state = TransferSessionState.CANCELLED
                return false
            }
        } else if (normalizedRemoteDeviceId != null) {
            session.boundPeerDeviceId = normalizedRemoteDeviceId
        }
        session.transferExpiresAtMillis = clock() + session.transferTtlMs
        return true
    }

    @Synchronized
    fun cancelSession(sessionId: String?) {
        val session = currentSession ?: return
        if (session.sessionId == sessionId) {
            if (session.state != TransferSessionState.COMPLETED) {
                session.state = TransferSessionState.CANCELLED
            }
        }
    }

    @Synchronized
    fun cancelActiveSessionIfMatches(
        accountId: String,
        extensionSessionId: String?,
    ): Boolean {
        val session = currentSession ?: return false
        if ((session.direction == TransferDirection.RESTORE_FROM_PC || session.direction == TransferDirection.TRANSFER_TO_PC) &&
            session.accountId == accountId && session.expectedExtensionSessionId != null &&
            session.expectedExtensionSessionId == extensionSessionId &&
            session.state != TransferSessionState.COMPLETED
        ) {
            session.state = TransferSessionState.CANCELLED
            return true
        }
        return false
    }

    @Synchronized
    fun completeSession(sessionId: String?): Boolean {
        val session = currentSession ?: return false
        expireIfNeeded(session)
        if (session.sessionId != sessionId || (session.state != TransferSessionState.CONSUMED && session.state != TransferSessionState.SENDING)) return false
        session.state = TransferSessionState.COMPLETED
        return true
    }

    @Synchronized
    fun getSessionState(sessionId: String?): TransferSessionState? {
        val session = currentSession ?: return null
        if (sessionId != null && session.sessionId != sessionId) return null
        expireIfNeeded(session)
        return session.state
    }

    private fun expireIfNeeded(session: TransferSession) {
        val deadline =
            if (session.state == TransferSessionState.SENDING && session.transferExpiresAtMillis != null) {
                session.transferExpiresAtMillis!!
            } else {
                session.expiresAtMillis
            }
        if (clock() <= deadline) return
        if (session.state == TransferSessionState.INITIATED || session.state == TransferSessionState.AWAITING_PAIRING ||
            session.state == TransferSessionState.SENDING || (session.state == TransferSessionState.CONSUMED && session.direction.isSending)
        ) {
            session.state = TransferSessionState.EXPIRED
        }
    }

    private fun parseIsoTimestampMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
