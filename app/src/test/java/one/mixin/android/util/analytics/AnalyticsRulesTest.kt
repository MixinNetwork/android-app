package one.mixin.android.util.analytics

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AnalyticsRulesTest {
    @Test
    fun marketShareEventUsesMarketShareNameAndParams() {
        val event =
            AnalyticsRules.marketShareEvent(
                type = AnalyticsTracker.MarketType.SPOT,
                target = AnalyticsTracker.MarketShareType.MIXIN_CONTACT,
            )

        assertEquals("market_share", event.name)
        assertEquals(
            mapOf(
                "type" to "spot",
                "target" to "mixin_contact",
            ),
            event.params,
        )
    }

    @Test
    fun marketDetailEventUsesTypeAndSource() {
        val event =
            AnalyticsRules.marketDetailEvent(
                type = AnalyticsTracker.MarketType.PERPS,
                source = AnalyticsTracker.MarketDetailSource.MARKETS_LIST,
            )

        assertEquals("market_detail", event.name)
        assertEquals(
            mapOf(
                "type" to "perps",
                "source" to "markets_list",
            ),
            event.params,
        )
    }

    @Test
    fun marketWatchlistEventUsesActionTypeAndSource() {
        val event =
            AnalyticsRules.marketWatchlistEvent(
                adding = false,
                type = AnalyticsTracker.MarketType.SPOT,
                source = AnalyticsTracker.MarketWatchlistSource.MARKET_DETAIL,
            )

        assertEquals("market_watchlist_remove", event.name)
        assertEquals(
            mapOf(
                "type" to "spot",
                "source" to "market_detail",
            ),
            event.params,
        )
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
    fun marketAndAssetVisibilityEventsDoNotSyncToAppsFlyer() {
        assertNull(AnalyticsRules.appsFlyerEventName("market_share"))
        assertNull(AnalyticsRules.appsFlyerEventName("hide_asset"))
        assertNull(AnalyticsRules.appsFlyerEventName("show_asset"))
    }

    @Test
    fun receiveAndPerpsEventsSyncToAppsFlyer() {
        assertEquals("asset_receive_success", AnalyticsRules.appsFlyerEventName("asset_receive_success"))
        assertEquals("trade_perps_open_start", AnalyticsRules.appsFlyerEventName("trade_perps_open_start"))
        assertEquals("trade_perps_open_end", AnalyticsRules.appsFlyerEventName("trade_perps_open_end"))
        assertEquals("trade_perps_close_start", AnalyticsRules.appsFlyerEventName("trade_perps_close_start"))
        assertEquals("trade_perps_close_end", AnalyticsRules.appsFlyerEventName("trade_perps_close_end"))
    }

    @Test
    fun nonOrganicConversionMapsSourceAndCampaign() {
        assertEquals(
            mapOf(
                "af_source" to "Non-organic",
                "af_media_source" to "example_media",
                "af_campaign" to "example_campaign",
            ),
            AnalyticsRules.conversionUserProperties(
                mapOf(
                    "af_status" to "Non-organic",
                    "media_source" to "example_media",
                    "campaign" to "example_campaign",
                ),
            ),
        )
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

    @Test
    fun receiveAssetLevelUsesV0ForNoMoney() {
        assertEquals("v0", AnalyticsRules.receiveAssetLevel("0".toBigDecimal()))
    }

    @Test
    fun receiveAssetLevelUsesV1ForPositiveAmountsUnderV100() {
        assertEquals("v1", AnalyticsRules.receiveAssetLevel("0.01".toBigDecimal()))
        assertEquals("v1", AnalyticsRules.receiveAssetLevel("99.99".toBigDecimal()))
    }
}
