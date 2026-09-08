package one.mixin.android.ui.wallet

import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionsFragmentTest {
    @Test
    fun resolveAssetDetailSourceUsesMarketDetailWhenOpenedFromMarket() {
        assertEquals(
            AnalyticsTracker.AssetSource.MARKET_DETAIL,
            resolveAssetDetailSource(fromMarket = true, source = AnalyticsTracker.AssetSource.TOKEN_LIST),
        )
    }

    @Test
    fun resolveAssetDetailSourceUsesProvidedSource() {
        assertEquals(
            AnalyticsTracker.AssetSource.TOKEN_LIST,
            resolveAssetDetailSource(fromMarket = false, source = AnalyticsTracker.AssetSource.TOKEN_LIST),
        )
    }

    @Test
    fun resolveAssetDetailSourceDefaultsToWalletHome() {
        assertEquals(
            AnalyticsTracker.AssetSource.WALLET_HOME,
            resolveAssetDetailSource(fromMarket = false, source = null),
        )
    }

    @Test
    fun visibilityActionRestoresMixedGroupAndKeepsNetworkScope() {
        val ethereum = token("ethereum", hidden = true)
        val base = token("base", hidden = false)
        val group = listOf(ethereum, base)

        assertEquals(false, shouldHideAssets(group))
        assertEquals(false, shouldHideAssets(group.filter { it.chainId == "ethereum" }))
        assertEquals(true, shouldHideAssets(group.filter { it.chainId == "base" }))
        assertEquals(false, shouldHideAssets(listOf(ethereum, base.copy(hidden = true))))
        assertEquals(true, shouldHideAssets(listOf(ethereum.copy(hidden = null), base)))
    }

    private fun token(id: String, hidden: Boolean?) = TokenItem(
        assetId = id,
        symbol = "USDC",
        name = "USD Coin",
        iconUrl = "",
        balance = "1",
        priceBtc = "0",
        priceUsd = "1",
        chainId = id,
        changeUsd = "0",
        changeBtc = "0",
        hidden = hidden,
        confirmations = 0,
        chainIconUrl = null,
        chainSymbol = null,
        chainName = null,
        assetKey = id,
        dust = null,
        withdrawalMemoPossibility = null,
        collectionHash = null,
        level = 0,
        precision = 6,
        coinId = "usd-coin",
    )
}
