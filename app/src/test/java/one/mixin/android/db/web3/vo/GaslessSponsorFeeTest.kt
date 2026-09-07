package one.mixin.android.db.web3.vo

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class GaslessSponsorFeeTest {
    @Test
    fun completeSponsorMetadataIsReturnedTogether() {
        val transaction = transaction(
            fee = "",
            sponsorFeeAssetId = "usdc",
            sponsorFeeAmount = "0.25",
        )

        assertEquals("usdc" to "0.25", transaction.getSponsorFee())
    }

    @Test
    fun networkFeeAloneDoesNotIdentifyGaslessPendingTransaction() {
        assertNull(transaction(fee = "0.25").getSponsorFee())
    }

    @Test
    fun incompleteSponsorMetadataIsNotReturned() {
        assertNull(transaction(fee = "", sponsorFeeAssetId = "usdc").getSponsorFee())
        assertNull(transaction(fee = "", sponsorFeeAmount = "0.25").getSponsorFee())
        assertNull(
            transaction(
                fee = "",
                sponsorFeeAssetId = " ",
                sponsorFeeAmount = "0.25",
            ).getSponsorFee(),
        )
    }

    @Test
    fun zeroSponsorAmountIsNotReturned() {
        listOf("0", "0.0", "0.00000000").forEach { amount ->
            assertNull(
                transaction(
                    fee = "",
                    sponsorFeeAssetId = "usdc",
                    sponsorFeeAmount = amount,
                ).getSponsorFee(),
            )
        }
    }

    @Test
    fun rebuiltPendingTransactionKeepsSponsorMetadata() {
        val source = transaction(
            fee = "",
            sponsorFeeAssetId = "usdc",
            sponsorFeeAmount = "0.25",
        )

        val (assetId, amount) = requireNotNull(source.getSponsorFee())
        val rebuilt = transaction(fee = "").copy(
            sponsorFeeAssetId = assetId,
            sponsorFeeAmount = amount,
        )

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
