package one.mixin.android.web3.swap

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapTokenSearchFilterTest {
    @Test
    fun remoteStockCategorySurvivesMergeWithOwnedToken() {
        val local = token("aaplx", "solana").copy(balance = "2", price = "100")
        val remote = local.copy(category = "stock")
        val result = mergeSwapTokenSearchResults(listOf(local), listOf(remote))
        assertEquals(1, result.size)
        assertEquals("stock", result.single().category)
        assertEquals("2", result.single().balance)
        assertEquals("100", result.single().price)
    }

    @Test
    fun remoteMatchSurvivesWhenLocalNameDidNotMatch() {
        val remote = token("aaplx", "solana").copy(category = "stock")
        val result = mergeSwapTokenSearchResults(emptyList(), listOf(remote))
        assertEquals(1, result.size)
        assertEquals("stock", result.single().category)
    }

    @Test
    fun localOnlyMatchesAndDifferentChainsAreKept() {
        val local = token("asset", "ethereum")
        val remote = token("asset", "solana")
        val result = mergeSwapTokenSearchResults(listOf(local), listOf(remote))
        assertEquals(listOf("solana", "ethereum"), result.map { it.chain.chainId })
        assertEquals(listOf(local), mergeSwapTokenSearchResults(listOf(local), emptyList()))
    }

    @Test
    fun ethereumAddressMakesXLayerSearchable() {
        assertTrue(
            isSwapSearchChainAvailable(
                chainId = Constants.ChainId.XLayer,
                addressChainIds = setOf(Constants.ChainId.ETHEREUM_CHAIN_ID),
            ),
        )
    }

    @Test
    fun pearlAddressMakesPearlSearchable() {
        assertTrue(
            isSwapSearchChainAvailable(
                chainId = Constants.ChainId.PEARL_CHAIN_ID,
                addressChainIds = setOf(Constants.ChainId.PEARL_CHAIN_ID),
            ),
        )
    }

    @Test
    fun pearlRemainsHiddenWithoutPearlAddress() {
        assertFalse(
            isSwapSearchChainAvailable(
                chainId = Constants.ChainId.PEARL_CHAIN_ID,
                addressChainIds = setOf(Constants.ChainId.BITCOIN_CHAIN_ID),
            ),
        )
    }

    @Test
    fun classicAndCommonWalletsKeepXLayerAndPearlWhenAddressesSupportThem() {
        val tokens = listOf(
            token("xlayer", Constants.ChainId.XLayer),
            token("pearl", Constants.ChainId.PEARL_CHAIN_ID),
        )

        val result = filterSwapTokensByWalletChains(
            tokens = tokens,
            addressChainIds = setOf(
                Constants.ChainId.ETHEREUM_CHAIN_ID,
                Constants.ChainId.PEARL_CHAIN_ID,
            ),
            inMixin = false,
        )

        assertEquals(listOf("xlayer", "pearl"), result.map { it.assetId })
    }

    @Test
    fun privacyWalletDoesNotApplyWeb3AddressChainFiltering() {
        val tokens = listOf(
            token("xlayer", Constants.ChainId.XLayer),
            token("pearl", Constants.ChainId.PEARL_CHAIN_ID),
        )

        val result = filterSwapTokensByWalletChains(
            tokens = tokens,
            addressChainIds = null,
            inMixin = true,
        )

        assertEquals(listOf("xlayer", "pearl"), result.map { it.assetId })
    }

    private fun token(assetId: String, chainId: String) =
        SwapToken(
            walletId = "wallet",
            address = "",
            assetId = assetId,
            decimals = 8,
            name = assetId,
            symbol = assetId,
            icon = "",
            chain = SwapChain(chainId, chainId, chainId, ""),
        )
}
