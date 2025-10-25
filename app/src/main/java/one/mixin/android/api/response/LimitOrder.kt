package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class LimitOrder(
    @SerializedName("limit_order_id")
    val limitOrderId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("receive_asset_id")
    val receiveAssetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("pending_amount")
    val pendingAmount: String,
    @SerializedName("expected_receive_amount")
    val expectedReceiveAmount: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("fund_status")
    val fundStatus: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("expired_at")
    val expiredAt: String
)