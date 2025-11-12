package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class LimitOrderRequest(
    @SerializedName("wallet_id")
    val walletId: String,
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
    @SerializedName("asset_destination")
    val assetDestination: String? = null,
    @SerializedName("receive_asset_destination")
    val receiveAssetDestination: String? = null,
)
