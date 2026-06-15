package one.mixin.android.util.analytics

import kotlin.test.assertEquals
import org.junit.Test

class AnalyticsRulesTest {
    @Test
    fun marketShareEventUsesShareMarketNameAndTypeParam() {
        val event = AnalyticsRules.marketShareEvent(AnalyticsTracker.MarketShareType.MIXIN_CONTACT)

        assertEquals("share_market", event.name)
        assertEquals(mapOf("type" to "mixin_contact"), event.params)
    }

    @Test
    fun hideAssetEventUsesWalletAndSourceParams() {
        val event = AnalyticsRules.assetVisibilityEvent(
            hidden = true,
            wallet = AnalyticsTracker.TradeWallet.MAIN,
            source = AnalyticsTracker.AssetSource.ASSET_DETAIL,
        )

        assertEquals("hide_asset", event.name)
        assertEquals(
            mapOf(
                "wallet" to "main",
                "source" to "asset_detail",
            ),
            event.params,
        )
    }

    @Test
    fun showAssetEventUsesOnlyWalletParam() {
        val event = AnalyticsRules.assetVisibilityEvent(
            hidden = false,
            wallet = AnalyticsTracker.TradeWallet.WEB3,
            source = AnalyticsTracker.AssetSource.WALLET_HOME,
        )

        assertEquals("show_asset", event.name)
        assertEquals(mapOf("wallet" to "web3"), event.params)
    }

    @Test
    fun marketAndAssetVisibilityEventsSyncToAppsFlyer() {
        assertEquals("share_market", AnalyticsRules.appsFlyerEventName("share_market"))
        assertEquals("hide_asset", AnalyticsRules.appsFlyerEventName("hide_asset"))
        assertEquals("show_asset", AnalyticsRules.appsFlyerEventName("show_asset"))
    }

    @Test
    fun spotOrdersEventUsesRenamedEventNameAndTypeParam() {
        val event = AnalyticsRules.spotOrdersEvent(AnalyticsTracker.SpotTradeType.ADVANCED)

        assertEquals("trade_spot_orders", event.name)
        assertEquals(mapOf("type" to "advanced"), event.params)
    }

    @Test
    fun spotOrderDetailEventUsesRenamedEventNameAndTypeParam() {
        val event = AnalyticsRules.spotOrderDetailEvent(AnalyticsTracker.SpotTradeType.SIMPLE)

        assertEquals("trade_spot_order_detail", event.name)
        assertEquals(mapOf("type" to "simple"), event.params)
    }
}
