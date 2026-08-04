package one.mixin.android.ui.home.web3.trade

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapToken

internal data class SwapTokenPair(
    val from: SwapToken?,
    val to: SwapToken?,
)

internal fun resolveDuplicateSwapTokenPair(
    tokens: List<SwapToken>,
    fromToken: SwapToken?,
    toToken: SwapToken?,
    keepToToken: Boolean,
): SwapTokenPair {
    if (fromToken?.getUnique() != toToken?.getUnique()) {
        return SwapTokenPair(fromToken, toToken)
    }

    val fallback = tokens.firstOrNull { t ->
        t.getUnique() != fromToken?.getUnique() && t.getUnique() in Constants.usdIds
    } ?: tokens.firstOrNull { t ->
        t.getUnique() != fromToken?.getUnique()
    }

    return if (keepToToken) {
        SwapTokenPair(fallback, toToken)
    } else {
        SwapTokenPair(fromToken, fallback)
    }
}
