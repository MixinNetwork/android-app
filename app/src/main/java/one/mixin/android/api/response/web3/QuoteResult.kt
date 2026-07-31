package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
data class QuoteResult(
    @SerializedName("inputMint")
    val inputMint: String,
    @SerializedName("inAmount")
    val inAmount: String,
    @SerializedName("outputMint")
    val outputMint: String,
    @SerializedName("outAmount")
    val outAmount: String,
    @SerializedName("slippage")
    val slippage: Int,
    @SerializedName("source")
    val source: String,
    @SerializedName("payload")
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
