package one.mixin.android.vo

import kotlin.test.Test
import kotlin.test.assertEquals
import one.mixin.android.vo.safe.RawTransaction
import one.mixin.android.vo.safe.RawTransactionState
import one.mixin.android.vo.safe.RawTransactionType
import one.mixin.android.vo.safe.SafeSnapshotType

class SnapshotItemTest {
    @Test
    fun `pending hash is shown when raw transaction is unspent`() {
        val snapshot = snapshotItem(type = SafeSnapshotType.snapshot.name, traceId = "trace-id")
        val rawTransaction = rawTransaction("trace-id", RawTransactionState.signed)

        assertEquals(true, snapshot.shouldShowPendingHash(rawTransaction))
    }

    @Test
    fun `pending hash is hidden when raw transaction was sent`() {
        val snapshot = snapshotItem(type = SafeSnapshotType.snapshot.name, traceId = "trace-id")
        val rawTransaction = rawTransaction("trace-id", RawTransactionState.spent)

        assertEquals(false, snapshot.shouldShowPendingHash(rawTransaction))
    }

    @Test
    fun `pending hash is hidden when raw transaction is missing`() {
        val snapshot = snapshotItem(type = SafeSnapshotType.withdrawal.name, traceId = "trace-id")

        assertEquals(false, snapshot.shouldShowPendingHash(null))
    }

    private fun rawTransaction(
        requestId: String,
        state: RawTransactionState,
    ) = RawTransaction(
        requestId = requestId,
        rawTransaction = "raw",
        receiverId = "",
        type = RawTransactionType.TRANSFER,
        state = state,
        createdAt = "2026-06-03T00:00:00Z",
        inscriptionHash = null,
    )

    private fun snapshotItem(
        type: String,
        traceId: String?,
    ) = SnapshotItem(
        snapshotId = "snapshot-id",
        type = type,
        assetId = "asset-id",
        amount = "-1",
        createdAt = "2026-06-03T00:00:00Z",
        opponentId = "opponent-id",
        opponentFullName = null,
        transactionHash = "hash",
        memo = null,
        assetSymbol = "XIN",
        confirmations = null,
        avatarUrl = null,
        assetConfirmations = 0,
        traceId = traceId,
        openingBalance = null,
        closingBalance = null,
        deposit = null,
        withdrawal = null,
        label = null,
        inscriptionHash = null,
        collectionHash = null,
        name = null,
        sequence = null,
        contentType = null,
        contentUrl = null,
        iconUrl = null,
    )
}
