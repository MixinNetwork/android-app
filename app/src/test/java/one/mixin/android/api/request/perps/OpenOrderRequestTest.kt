package one.mixin.android.api.request.perps

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OpenOrderRequestTest {
    @Test
    fun serializesLeaderPositionId() {
        val leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1"

        val json = Gson().toJsonTree(openOrderRequest(leaderPositionId)).asJsonObject

        assertEquals(leaderPositionId, json.get("leader_position_id").asString)
        assertFalse(json.has("leader_position"))
        assertFalse(json.has("position_id"))
    }

    @Test
    fun omitsLeaderPositionIdForRegularOrder() {
        val json = Gson().toJsonTree(openOrderRequest()).asJsonObject

        assertFalse(json.has("leader_position_id"))
    }

    private fun openOrderRequest(leaderPositionId: String? = null) =
        OpenOrderRequest(
            assetId = "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
            marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119",
            side = "long",
            amount = "10",
            leverage = 5,
            walletId = "41d16c28-0c3a-493d-a2b4-b57875371abf",
            leaderPositionId = leaderPositionId,
        )
}
