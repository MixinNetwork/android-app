package one.mixin.android.db.web3.vo

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class GaslessSponsorFeeTest {
    @Test
    fun sponsorMetadataIdentifiesGaslessPendingTransaction() {
        val transaction = transaction(
            fee = "",
            sponsorFeeAssetId = "usdc",
            sponsorFeeAmount = "0.25",
        )

        assertTrue(transaction.hasSponsorFeeMetadata())
    }

    @Test
    fun networkFeeAloneDoesNotIdentifyGaslessPendingTransaction() {
        assertFalse(transaction(fee = "0.25").hasSponsorFeeMetadata())
    }

    @Test
    fun rebuiltPendingTransactionKeepsSponsorMetadata() {
        val source = transaction(
            fee = "",
            sponsorFeeAssetId = "usdc",
            sponsorFeeAmount = "0.25",
        )

        val rebuilt = transaction(fee = "").copySponsorFeeFrom(source)

        assertEquals("usdc", rebuilt.sponsorFeeAssetId)
        assertEquals("0.25", rebuilt.sponsorFeeAmount)
    }

    private fun transaction(
        fee: String,
        sponsorFeeAssetId: String? = null,
        sponsorFeeAmount: String? = null,
    ) = Web3Transaction(
        transactionHash = "hash",
        chainId = "chain",
        address = "address",
        transactionType = TransactionType.TRANSFER_OUT.value,
        status = TransactionStatus.PENDING.value,
        blockNumber = 0,
        fee = fee,
        sponsorFeeAssetId = sponsorFeeAssetId,
        sponsorFeeAmount = sponsorFeeAmount,
        senders = emptyList(),
        receivers = emptyList(),
        transactionAt = "2026-08-12T00:00:00Z",
        createdAt = "2026-08-12T00:00:00Z",
        updatedAt = "2026-08-12T00:00:00Z",
    )
}
