package one.mixin.android.ui.conversation

import one.mixin.android.extension.PerpsTradeAction
import one.mixin.android.extension.SpotTradeAction
import one.mixin.android.extension.toPerpsTradeAction
import one.mixin.android.extension.toSpotTradeAction
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
}
