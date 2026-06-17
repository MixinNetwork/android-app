package one.mixin.android.api.response.web3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
data class QuoteResult(
    val inputMint: String,
    val inAmount: String,
    val outputMint: String,
    val outAmount: String,
    val slippage: Int,
    val source: String,
    val payload: String,
): Parcelable

fun QuoteResult?.rate(fromToken: SwapToken?, toToken: SwapToken?): BigDecimal {
    if (this == null) return BigDecimal.ZERO
    if (fromToken == null || toToken == null) return BigDecimal.ZERO
    return runCatching {
        val inValue = inAmount.toBigDecimal()
        val outValue = outAmount.toBigDecimal()
        outValue.divide(inValue, 8, RoundingMode.CEILING)
    }.getOrDefault(BigDecimal.ZERO)
}
