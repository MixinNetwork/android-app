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
    // Display the token on the right side of price ratio (after '=')
    // When inverted: "1 toToken = X fromToken" -> show fromToken (right side)
    // When not inverted: "1 fromToken = X toToken" -> show toToken (right side)
    val displayToken: SwapToken?
        get() = if (isPriceInverted) fromToken else toToken

    val displayChainName: String
        get() = if (isPriceInverted) fromToken?.chain?.name ?: "" else toToken?.chain?.name ?: ""

    val displayTokenName: String
        get() = if (isPriceInverted) fromToken?.name ?: "" else toToken?.name ?: ""

    fun formatPriceRatio(displayPrice: String): String? {
        val price = displayPrice.toBigDecimalOrNull()
        return if (price != null && price > BigDecimal.ZERO) {
            if (isPriceInverted) {
                "1 ${toToken?.symbol} = ${price.stripTrailingZeros().toPlainString()} ${fromToken?.symbol}"
            } else {
                "1 ${fromToken?.symbol} = ${price.stripTrailingZeros().toPlainString()} ${toToken?.symbol}"
            }
        } else {
            "-"
        }
    }
}
