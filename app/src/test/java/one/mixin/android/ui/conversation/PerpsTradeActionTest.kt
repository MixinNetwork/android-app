package one.mixin.android.ui.conversation

import one.mixin.android.extension.PerpsTradeAction
import one.mixin.android.extension.SpotTradeAction
import one.mixin.android.extension.toPerpsTradeAction
import one.mixin.android.extension.toSpotTradeAction
import one.mixin.android.ui.wallet.WalletHomeBannerActionTarget
import one.mixin.android.ui.wallet.toClassicWalletHomeBannerActionTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PerpsTradeActionTest {
    @Test
    fun parsesSwapTradeAction() {
        val action = "https://mixin.one/swap?input=43d61dcd-e413-450d-80b8-101d5e903357&output=c6d0c728-2624-429b-8e0d-d9d19b6592fa&amount=1.2&referral=7000"

        assertEquals(
            SpotTradeAction(
                input = "43d61dcd-e413-450d-80b8-101d5e903357",
                output = "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
                amount = "1.2",
                referral = "7000",
                openLimit = false,
            ),
            action.toSpotTradeAction(),
        )
    }

    @Test
    fun parsesLimitTradeAction() {
        val action = "https://mixin.one/trade?type=limit&input=43d61dcd-e413-450d-80b8-101d5e903357&output=c6d0c728-2624-429b-8e0d-d9d19b6592fa"

        assertEquals(
            SpotTradeAction(
                input = "43d61dcd-e413-450d-80b8-101d5e903357",
                output = "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
                amount = null,
                referral = null,
                openLimit = true,
            ),
            action.toSpotTradeAction(),
        )
    }

    @Test
    fun parsesHttpsPerpsTradeAction() {
        val action = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119"

        assertEquals("e015f42e-b0ff-38e7-87b1-7e8d46fea119", action.toPerpsTradeAction()?.marketId)
    }

    @Test
    fun parsesMixinPerpetualTradeAction() {
        val action = "mixin://mixin.one/trade?type=perpetual&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119"

        assertEquals("e015f42e-b0ff-38e7-87b1-7e8d46fea119", action.toPerpsTradeAction()?.marketId)
    }

    @Test
    fun parsesPerpsTradeActionWithoutMarket() {
        val action = "https://mixin.one/trade?type=perps"

        assertNotNull(action.toPerpsTradeAction())
        assertEquals(null, action.toPerpsTradeAction()?.marketId)
    }

    @Test
    fun ignoresNonPerpsTradeAction() {
        val action = "https://mixin.one/trade?type=limit&input=abc&output=def"

        assertNull(action.toPerpsTradeAction())
    }

    @Test
    fun ignoresSpotTradeActionWithoutLocalTokenParameters() {
        val action = "https://mixin.one/trade?type=limit&input=abc&output=def"

        assertNull(action.toSpotTradeAction())
    }

    @Test
    fun ignoresMalformedEncodedTradeAction() {
        val action = "https://mixin.one/trade?type=%ZZ&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119"

        assertNull(action.toPerpsTradeAction())
        assertNull(action.toSpotTradeAction())
    }

    @Test
    fun classicBannerActionParsesSpotTradeTarget() {
        val action = "https://mixin.one/swap?input=43d61dcd-e413-450d-80b8-101d5e903357&output=c6d0c728-2624-429b-8e0d-d9d19b6592fa&amount=1.2&referral=7000"

        val target = action.toClassicWalletHomeBannerActionTarget()

        assertEquals(
            WalletHomeBannerActionTarget.SpotTrade(
                SpotTradeAction(
                    input = "43d61dcd-e413-450d-80b8-101d5e903357",
                    output = "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
                    amount = "1.2",
                    referral = "7000",
                    openLimit = false,
                ),
            ),
            target,
        )
    }

    @Test
    fun classicBannerActionParsesPerpsAndBuyTargets() {
        assertEquals(
            WalletHomeBannerActionTarget.PerpsMarket("e015f42e-b0ff-38e7-87b1-7e8d46fea119"),
            "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119".toClassicWalletHomeBannerActionTarget(),
        )
        assertEquals(
            WalletHomeBannerActionTarget.PerpsTab,
            "https://mixin.one/trade?type=perps".toClassicWalletHomeBannerActionTarget(),
        )
        assertEquals(
            WalletHomeBannerActionTarget.Buy,
            "https://mixin.one/buy".toClassicWalletHomeBannerActionTarget(),
        )
    }

    @Test
    fun classicBannerActionFallsBackToWebTarget() {
        val url = "https://mixin.one/users/41d16c28-0c3a-493d-a2b4-b57875371abf"

        assertEquals(WalletHomeBannerActionTarget.Web(url), url.toClassicWalletHomeBannerActionTarget())
    }
}
