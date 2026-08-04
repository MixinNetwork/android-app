package one.mixin.android.ui.home.web3.trade

import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import kotlin.test.Test
import kotlin.test.assertEquals

class SwapTokenSelectionTest {
    @Test
    fun outputArgumentKeepsTargetAndMovesDuplicateFromToBaseToken() {
        val output = token("output")
        val base = token(USDT_ASSET_ETH_ID)

        val result = resolveDuplicateSwapTokenPair(
            tokens = listOf(output, base),
            fromToken = output,
            toToken = output,
            keepToToken = true,
        )

        assertEquals(base.assetId, result.from?.assetId)
        assertEquals(output.assetId, result.to?.assetId)
    }

    @Test
    fun missingOutputArgumentKeepsFromAndMovesDuplicateToToken() {
        val from = token("from")
        val base = token(USDT_ASSET_ETH_ID)

        val result = resolveDuplicateSwapTokenPair(
            tokens = listOf(from, base),
            fromToken = from,
            toToken = from,
            keepToToken = false,
        )

        assertEquals(from.assetId, result.from?.assetId)
        assertEquals(base.assetId, result.to?.assetId)
    }

    private fun token(assetId: String) =
        SwapToken(
            walletId = null,
            address = "",
            assetId = assetId,
            decimals = 8,
            name = assetId,
            symbol = assetId,
            icon = "",
            chain = SwapChain("", "", "", ""),
        )
}
