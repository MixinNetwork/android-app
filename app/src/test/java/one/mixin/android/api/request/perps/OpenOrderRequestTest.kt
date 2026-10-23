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

    @Test
    fun increaseUsesRequestTypeAndPreservesWeb3Destination() {
        val json = Gson().toJsonTree(AdjustMarginRequest("increase", "1.00000001", "asset-id", "wallet-address")).asJsonObject
        assertEquals("increase", json["type"].asString)
        assertEquals("1.00000001", json["amount"].asString)
        assertEquals("asset-id", json["asset_id"].asString)
        assertEquals("wallet-address", json["destination"].asString)
        assertFalse(json.has("quantity"))
        assertFalse(json.has("leverage"))
    }

    @Test
    fun decreaseOmitsPaymentAssetAndDestination() {
        val json = Gson().toJsonTree(AdjustMarginRequest("decrease", "2.5")).asJsonObject
        assertEquals(setOf("type", "amount"), json.keySet())
        assertEquals("decrease", json["type"].asString)
        assertEquals("2.5", json["amount"].asString)
    }

    @Test
    fun serializesLeaderPositionIdForIncreaseOrder() {
        val request = IncreaseOrderRequest(
            assetId = "asset",
            amount = "12.5",
            leaderPositionId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1",
        )
        val json = Gson().toJsonTree(request).asJsonObject
        assertEquals(request.leaderPositionId, json.get("leader_position_id").asString)
        assertEquals("12.5", json.get("amount").asString)
        assertFalse(json.has("leader_position"))
    }

    @Test
    fun ordinaryIncreaseOrderOmitsLeaderPositionId() {
        val json = Gson().toJsonTree(IncreaseOrderRequest(assetId = "asset", amount = "10")).asJsonObject
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
