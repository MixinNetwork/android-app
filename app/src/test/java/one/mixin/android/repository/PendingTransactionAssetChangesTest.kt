package one.mixin.android.repository

import one.mixin.android.api.response.web3.BalanceChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PendingTransactionAssetChangesTest {
    @Test
    fun swapPutsNegativeAmountsInSendersAndPositiveInReceivers() {
        val changes = pendingAssetChanges(
            listOf(
                balanceChange(assetId = "usdt", amount = "5", from = "wallet", to = "wallet"),
                balanceChange(assetId = "aeth-usdt", amount = "-4.999998", from = "wallet", to = null),
            ),
        )

        assertEquals(listOf("aeth-usdt"), changes.senders.map { it.assetId })
        assertEquals(listOf("4.999998"), changes.senders.map { it.amount })
        assertEquals(listOf("usdt"), changes.receivers.map { it.assetId })
        assertEquals(listOf("5"), changes.receivers.map { it.amount })
    }

    @Test
    fun solanaSwapDoesNotMirrorEachAsset() {
        val changes = pendingAssetChanges(
            listOf(
                balanceChange(assetId = "usdc", amount = "10"),
                balanceChange(assetId = "jiusdc", amount = "-9.446338"),
            ),
        )

        assertEquals(1, changes.senders.size)
        assertEquals(1, changes.receivers.size)
        assertEquals("jiusdc", changes.senders.single().assetId)
        assertEquals("9.446338", changes.senders.single().amount)
        assertEquals("usdc", changes.receivers.single().assetId)
        assertEquals("10", changes.receivers.single().amount)
    }

    @Test
    fun zeroAndInvalidAmountsAreSkipped() {
        val changes = pendingAssetChanges(
            listOf(
                balanceChange(assetId = "zero", amount = "0"),
                balanceChange(assetId = "blank", amount = ""),
                balanceChange(assetId = "nan", amount = "abc"),
            ),
        )

        assertTrue(changes.senders.isEmpty())
        assertTrue(changes.receivers.isEmpty())
    }

    @Test
    fun nullBalanceChangesAreEmpty() {
        val changes = pendingAssetChanges(null)

        assertTrue(changes.senders.isEmpty())
        assertTrue(changes.receivers.isEmpty())
    }

    private fun balanceChange(
        assetId: String,
        amount: String,
        from: String? = "from",
        to: String? = "to",
    ) = BalanceChange(
        assetId = assetId,
        address = "asset-key",
        amount = amount,
        decimals = 6,
        name = assetId,
        symbol = assetId,
        icon = null,
        from = from,
        to = to,
    )
}
