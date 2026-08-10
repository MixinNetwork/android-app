package one.mixin.android.ui.home.web3.trade

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapToken

internal data class SwapTokenPair(
    val from: SwapToken?,
    val to: SwapToken?,
)

internal fun resolveDefaultWeb3SwapTokenPair(
    tokens: List<SwapToken>,
    fromToken: SwapToken?,
    toToken: SwapToken?,
): SwapTokenPair {
    if (tokens.isEmpty()) return SwapTokenPair(null, null)
    val resolvedFrom = fromToken ?: tokens[0]
    val resolvedTo = if (toToken == null || toToken.getUnique() == resolvedFrom.getUnique()) {
        tokens.firstOrNull { token -> token.assetId != resolvedFrom.assetId }
            ?: tokens.getOrNull(1)
            ?: tokens[0]
    } else {
        toToken
    }
    return SwapTokenPair(resolvedFrom, resolvedTo)
}

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
