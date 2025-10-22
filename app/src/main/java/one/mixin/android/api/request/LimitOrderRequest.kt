package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class LimitOrderRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("receive_asset_id")
    val receiveAssetId: String,
    @SerializedName("expected_receive_amount")
    val expectedReceiveAmount: String,
    @SerializedName("expired_at")
    val expiredAt: String,
)
