package one.mixin.android.tip.wc.internal

import com.google.gson.annotations.SerializedName
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
    val feeRate: BigDecimal,
    val feeBtc: BigDecimal,
) {
    fun permitsFee(finalFeeBtc: BigDecimal): Boolean =
        feeRate > BigDecimal.ZERO && feeRate <= MAX_FEE_RATE_SAT_PER_VB &&
            feeBtc > BigDecimal.ZERO && finalFeeBtc > BigDecimal.ZERO && finalFeeBtc <= feeBtc

    companion object {
        val MAX_FEE_RATE_SAT_PER_VB = BigDecimal("100")

        fun cappedFeeRate(feeRate: BigDecimal, minFeeBtc: BigDecimal?, virtualSize: Int): BigDecimal {
            require(feeRate > BigDecimal.ZERO && virtualSize > 0)
            require(minFeeBtc == null || minFeeBtc >= BigDecimal.ZERO)
            val minimumRate = minFeeBtc?.multiply(BigDecimal("100000000"))
                ?.divide(BigDecimal(virtualSize), 8, RoundingMode.UP) ?: BigDecimal.ZERO
            return feeRate.max(minimumRate).min(MAX_FEE_RATE_SAT_PER_VB)
        }
    }
}

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
    @SerializedName("address")
    val address: String,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("messageHash")
    val messageHash: String? = null,
)
