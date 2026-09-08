package one.mixin.android.web3.swap

import android.app.Application
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SwapTokenGroupingTest {
    @Test
    fun recentTokenFindsAvailableNetworksAfterItsOriginalNetworkIsSpent() {
        val recent = token("ethereum", "0")
        val base = token("base", "2")
        val polygon = token("polygon", "3")
        val adapter = SwapTokenAdapter(grouped = true).apply {
            tokens = listOf(base, polygon, base.copy(walletId = "another-wallet"))
        }

        assertEquals(listOf(base, polygon), adapter.groupAssets(recent))
        adapter.chain = "base"
        assertEquals(listOf(base), adapter.groupAssets(recent))
        assertTrue(adapter.groupAssets(recent.copy(collectionHash = "collection")).isEmpty())
        assertTrue(adapter.groupAssets(recent.copy(coinId = null)).isEmpty())
    }

    @Test
    fun recentAssetMatchesBeforeItsCoinMetadataIsAvailable() {
        val ethereum = token("ethereum", "3")
        val base = token("base", "2")
        val adapter = SwapTokenAdapter(grouped = true).apply {
            tokens = listOf(ethereum, base)
        }

        assertEquals(listOf(ethereum, base), adapter.groupAssets(ethereum.copy(coinId = null)))
    }

    @Test
    fun ungroupedTokensKeepNetworksIndependent() {
        val adapter = SwapTokenAdapter().apply {
            tokens = listOf(token("base", "2"), token("polygon", "3"))
        }

        assertEquals(2, adapter.itemCount)
        assertTrue(adapter.groupAssets(token("ethereum", "0")).isEmpty())
    }

    private fun token(assetId: String, balance: String) = SwapToken(
        walletId = null,
        address = assetId,
        assetId = assetId,
        decimals = 6,
        name = "USD Coin",
        symbol = "USDC",
        icon = "",
        chain = SwapChain(assetId, assetId, assetId, ""),
        balance = balance,
        coinId = "usd-coin",
    )
}
