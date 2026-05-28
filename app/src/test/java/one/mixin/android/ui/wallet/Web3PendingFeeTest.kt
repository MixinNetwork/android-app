package one.mixin.android.ui.wallet

import one.mixin.android.db.web3.vo.Web3TransactionItem
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class Web3PendingFeeTest {
    @Test
    fun gaslessPendingFeeKeepsDisplayableAmount() {
        assertEquals("0.00021", normalizeGaslessPendingFeeAmount("0.0002100"))
    }

    @Test
    fun gaslessPendingFeeFallsBackToEmptyWhenMissing() {
        assertEquals("", normalizeGaslessPendingFeeAmount(null))
    }

    @Test
    fun sponsorFeeIsPreferredForDisplay() {
        val transaction = transactionItem(
            fee = "0.0001",
            sponsorFeeAmount = "0.25",
            sponsorFeeAssetSymbol = "USDT",
        )

        assertEquals("0.25", transaction.displayFeeAmount())
        assertEquals("USDT", transaction.displayFeeSymbol())
        assertTrue(transaction.hasSponsorFee())
    }

    @Test
    fun chainFeeIsUsedWhenSponsorFeeIsMissing() {
        val transaction = transactionItem(
            fee = "0.0001",
            chainSymbol = "ETH",
        )

        assertEquals("0.0001", transaction.displayFeeAmount())
        assertEquals("ETH", transaction.displayFeeSymbol())
    }

    private fun transactionItem(
        fee: String,
        sponsorFeeAmount: String? = null,
        chainSymbol: String? = null,
        sponsorFeeAssetSymbol: String? = null,
    ) = Web3TransactionItem(
        transactionHash = "hash",
        transactionType = "transfer_in",
        status = "success",
        blockNumber = 1,
        chainId = "chain",
        address = "address",
        fee = fee,
        sponsorFeeAssetId = if (sponsorFeeAmount.isNullOrBlank()) null else "sponsor",
        sponsorFeeAmount = sponsorFeeAmount,
        senders = emptyList(),
        receivers = emptyList(),
        approvals = null,
        sendAssetId = null,
        receiveAssetId = null,
        transactionAt = "2026-04-03T14:52:35.000000Z",
        updatedAt = "2026-04-03T14:52:55.822512Z",
        chainSymbol = chainSymbol,
        sponsorFeeAssetSymbol = sponsorFeeAssetSymbol,
        level = 0,
    )
}
