package one.mixin.android.web3.swap

import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwapTokenAdapterTest {
    @Test
    fun sameAssetWithUpdatedCategoryAppearsInStockSearch() {
        val adapter = SwapTokenAdapter()
        val local = SwapToken(
            walletId = "wallet",
            address = "address",
            assetId = "aaplx",
            decimals = 8,
            name = "Apple",
            symbol = "AAPLx",
            icon = "",
            chain = SwapChain("solana", "Solana", "SOL", ""),
        )
        adapter.chain = ""
        adapter.keyword = "AAPL"
        adapter.tokens = listOf(local)
        assertEquals(0, adapter.itemCount)
        adapter.tokens = mergeSwapTokenSearchResults(listOf(local), listOf(local.copy(category = "stock")))
        assertEquals(1, adapter.itemCount)
        assertEquals("stock", adapter.tokens.single().category)
    }
}
