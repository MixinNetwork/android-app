package one.mixin.android.ui.conversation

import one.mixin.android.extension.PerpsTradeAction
import one.mixin.android.extension.PerpsOpenPositionAction
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
    fun parsesPerpsLeaderPositionId() {
        val action = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1"

        assertEquals(
            PerpsTradeAction(
                marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119",
                leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
            ),
            action.toPerpsTradeAction(),
        )
    }

    @Test
    fun rejectsInvalidPerpsLeaderPositionId() {
        val action = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&leader_position=invalid"

        assertNull(action.toPerpsTradeAction())
    }

    @Test
    fun rejectsMalformedEncodedPerpsLeaderPositionId() {
        val action = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&leader_position=%ZZ"

        assertNull(action.toPerpsTradeAction())
    }

    @Test
    fun rejectsPerpsLeaderPositionIdWithoutMarket() {
        val action = "https://mixin.one/trade?type=perps&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1"

        assertNull(action.toPerpsTradeAction())
    }

    @Test
    fun parsesDirectPerpsOpenAction() {
        val action = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&action=open&side=short&leverage=10&margin=10&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1"

        assertEquals(
            PerpsTradeAction(
                marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119",
                leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
                openPosition = PerpsOpenPositionAction(
                    isLong = false,
                    leverage = 10,
                    margin = "10",
                ),
            ),
            action.toPerpsTradeAction(),
        )
    }

    @Test
    fun directPerpsOpenRequiresOpenActionAndValidSide() {
        val base = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1"

        assertNull("$base&side=long".toPerpsTradeAction()?.openPosition)
        assertNull("$base&action=open".toPerpsTradeAction()?.openPosition)
        assertNull("$base&action=close&side=long".toPerpsTradeAction()?.openPosition)
        assertNull("$base&action=open&side=invalid".toPerpsTradeAction()?.openPosition)
        assertEquals(
            "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
            "$base&action=open".toPerpsTradeAction()?.leaderPositionId,
        )
    }

    @Test
    fun directPerpsOpenAcceptsOptionalValues() {
        val action = "mixin://mixin.one/trade?type=perpetual&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&action=OPEN&side=LONG"

        assertEquals(
            PerpsOpenPositionAction(
                isLong = true,
                leverage = null,
                margin = null,
            ),
            action.toPerpsTradeAction()?.openPosition,
        )
    }

    @Test
    fun rejectsInvalidDirectPerpsOpenDefaults() {
        val base = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&action=open&side=long"

        assertNull("$base&leverage=0".toPerpsTradeAction())
        assertNull("$base&leverage=1.5".toPerpsTradeAction())
        assertNull("$base&margin=0".toPerpsTradeAction())
        assertNull("$base&margin=-1".toPerpsTradeAction())
        assertNull("$base&margin=0.000000001".toPerpsTradeAction())
        assertNull("$base&margin=1e2".toPerpsTradeAction())
        assertNull("$base&margin=${"1".repeat(65)}".toPerpsTradeAction())
    }

    @Test
    fun detailPerpsActionKeepsLeaderAndIgnoresOpenDefaults() {
        val action = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&leverage=invalid&margin=invalid&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1"

        assertEquals(
            PerpsTradeAction(
                marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119",
                leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
            ),
            action.toPerpsTradeAction(),
        )
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
    fun classicBannerActionKeepsPerpsLeaderPositionId() {
        assertEquals(
            WalletHomeBannerActionTarget.PerpsMarket(
                marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119",
                leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
            ),
            "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1".toClassicWalletHomeBannerActionTarget(),
        )
    }

    @Test
    fun classicBannerActionParsesDirectPerpsOpenTarget() {
        assertEquals(
            WalletHomeBannerActionTarget.PerpsOpen(
                marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119",
                isLong = true,
                leverage = 10,
                margin = "25.5",
                leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
            ),
            "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&action=open&side=long&leverage=10&margin=25.5&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1".toClassicWalletHomeBannerActionTarget(),
        )
    }

    @Test
    fun classicBannerActionFallsBackToWebTarget() {
        val url = "https://mixin.one/users/41d16c28-0c3a-493d-a2b4-b57875371abf"

        assertEquals(WalletHomeBannerActionTarget.Web(url), url.toClassicWalletHomeBannerActionTarget())
    }
}
