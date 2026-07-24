package one.mixin.android.tip.wc.internal

import java.math.BigDecimal
import java.math.RoundingMode

data class WcBitcoinGetAccountAddresses(
    val account: String,
    val intentions: List<String>? = null,
)

data class WcBitcoinSendTransfer(
    val account: String,
    val recipientAddress: String,
    val amount: String,
    val changeAddress: String? = null,
    val memo: String? = null,
) {
    fun amountBtc(): String {
        val sats = amount.toBigDecimal()
        require(sats > BigDecimal.ZERO && sats.stripTrailingZeros().scale() <= 0) {
            "Invalid Bitcoin amount"
        }
        return sats.divide(SATOSHIS_PER_BTC, 8, RoundingMode.UNNECESSARY)
            .stripTrailingZeros()
            .toPlainString()
    }

    private companion object {
        val SATOSHIS_PER_BTC = BigDecimal("100000000")
    }
}

data class WcBitcoinSignedTransfer(
    val rawTx: String,
    val fromAddress: String,
    val recipientAddress: String,
    val consumedOutputIds: List<String>,
    val feeRate: BigDecimal?,
)

data class WcBitcoinFeeEstimate(
    val feeRate: BigDecimal?,
    val minFee: String?,
)

data class WcBitcoinSignMessage(
    val account: String,
    val message: String,
    val address: String? = null,
    val protocol: String? = null,
)

data class WcBitcoinAccountAddress(
    val address: String,
    val publicKey: String? = null,
    val path: String? = null,
    val intention: String = "payment",
)

data class WcBitcoinSignature(
    val address: String,
    val signature: String,
    val messageHash: String? = null,
)
