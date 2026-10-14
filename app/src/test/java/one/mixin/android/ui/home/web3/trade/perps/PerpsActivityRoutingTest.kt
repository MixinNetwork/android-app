package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.extension.toPerpsTradeAction
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerpsActivityRoutingTest {
    @Test
    fun existingPositionKeepsMarketDetail() {
        assertFalse(canOpenNewPerpsPosition(hasOpenPosition = true))
    }

    @Test
    fun missingPositionOpensNewPositionPage() {
        assertTrue(canOpenNewPerpsPosition(hasOpenPosition = false))
    }

    @Test
    fun completeLinkCanPreviewBothDirectionsWithoutLeaderId() {
        for (side in listOf("long", "short")) {
            val action = assertNotNull("$link&side=$side&leverage=10&margin=10.5".toPerpsTradeAction()?.openPosition)
            assertTrue(canPreviewPerpsLinkOrder(action.isLong, action.leverage, action.margin))
        }
    }

    @Test
    fun incompleteLinkKeepsOpenPositionPage() {
        for (parameters in listOf("", "&side=long", "&side=short&leverage=10", "&side=long&margin=10", "&leverage=10&margin=10")) {
            val action = assertNotNull("$link$parameters".toPerpsTradeAction()?.openPosition)
            assertFalse(canPreviewPerpsLinkOrder(action.isLong, action.leverage, action.margin))
        }
    }

    @Test
    fun invalidValuesCannotCreateAnOrder() {
        assertFalse(canPreviewPerpsLinkOrder(true, 0, "10"))
        assertFalse(canPreviewPerpsLinkOrder(true, -1, "10"))
        for (margin in listOf("", "invalid", "0", "-1")) {
            assertFalse(canPreviewPerpsLinkOrder(false, 10, margin))
        }
    }

    @Test
    fun matchingLeaderCanAddWithMatchingOrOmittedLeverage() {
        assertTrue(canAddPerpsLeaderPosition(position("long"), true, 10))
        assertTrue(canAddPerpsLeaderPosition(position("short"), false, 10))
        assertTrue(canAddPerpsLeaderPosition(position("short"), false, null))
    }

    @Test
    fun oppositeDirectionOrDifferentLeverageCannotAdd() {
        assertFalse(canAddPerpsLeaderPosition(position("long"), false, 10))
        assertFalse(canAddPerpsLeaderPosition(position("short"), true, null))
        assertFalse(canAddPerpsLeaderPosition(position("long"), true, 5))
    }

    @Test
    fun pendingOrMissingPositionCannotAdd() {
        for (state in listOf(PerpsPosition.STATE_OPENING, PerpsPosition.STATE_ADDING, "closed", null)) {
            assertFalse(canAddPerpsLeaderPosition(position("long").copy(state = state), true, 10))
        }
        assertFalse(canAddPerpsLeaderPosition(null, true, 10))
    }

    @Test
    fun incompleteLinkWithMarginKeepsLeaderAndAllowsMatchingAdd() {
        val action = assertNotNull("$link&side=short&margin=12.5&leader_position=45d4c134-5682-4b1a-baf5-7c73b1590cc1".toPerpsTradeAction())
        val open = assertNotNull(action.openPosition)
        assertFalse(canPreviewPerpsLinkOrder(open.isLong, open.leverage, open.margin))
        assertTrue(canAddPerpsLeaderPosition(position("short"), assertNotNull(open.isLong), open.leverage))
        kotlin.test.assertEquals("12.5", open.margin)
        kotlin.test.assertEquals("45d4c134-5682-4b1a-baf5-7c73b1590cc1", action.leaderPositionId)
    }

    private fun position(side: String) = PerpsPositionItem(
        positionId = "position",
        marketId = "market",
        side = side,
        quantity = "1",
        entryPrice = "100",
        leverage = 10,
        state = PerpsPosition.STATE_OPEN,
    )

    private val link = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&action=open"

}
