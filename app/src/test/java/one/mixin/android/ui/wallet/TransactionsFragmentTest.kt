package one.mixin.android.ui.wallet

import one.mixin.android.util.analytics.AnalyticsTracker
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
}
