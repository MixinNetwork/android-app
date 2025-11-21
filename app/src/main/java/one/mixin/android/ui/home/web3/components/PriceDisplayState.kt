package one.mixin.android.ui.home.web3.components

import one.mixin.android.api.response.web3.SwapToken
import java.math.BigDecimal
import java.math.RoundingMode

data class PriceDisplayState(
    val fromToken: SwapToken?,
    val toToken: SwapToken?,
    val isPriceInverted: Boolean,
    val isPriceLoading: Boolean,
) {
    val displayToken: SwapToken?
        get() = if (isPriceInverted) toToken else fromToken

    val displayChainName: String
        get() = if (isPriceInverted) toToken?.chain?.name ?: "" else fromToken?.chain?.name ?: ""

    val displayTokenName: String
        get() = if (isPriceInverted) toToken?.name ?: "" else fromToken?.name ?: ""

    fun formatPriceRatio(displayPrice: String): String? {
        val price = displayPrice.toBigDecimalOrNull()
        return if (price != null && price > BigDecimal.ZERO) {
            if (isPriceInverted) {
                "1 ${toToken?.symbol} = ${price.stripTrailingZeros().toPlainString()} ${fromToken?.symbol}"
            } else {
                "1 ${fromToken?.symbol} = ${price.stripTrailingZeros().toPlainString()} ${toToken?.symbol}"
            }
        } else {
            null
        }
    }
}
