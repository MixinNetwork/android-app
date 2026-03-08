package one.mixin.android.api.request.perps

import com.google.gson.annotations.SerializedName

data class OpenOrderRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("side")
    val side: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("leverage")
    val leverage: Int,
    @SerializedName("wallet_id")
    val walletId: String,
    @SerializedName("destination")
    val destination: String? = null
)

data class OpenOrderResponse(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("pay_url")
    val payUrl: String?,
    @SerializedName("pay_amount")
    val payAmount: String?,
    @SerializedName("deposit_destination")
    val depositDestination: String?,
    @SerializedName("app_id")
    val appId: String?
)

data class CloseOrderRequest(
    @SerializedName("position_id")
    val positionId: String
)

data class CloseOrderResponse(
    @SerializedName("order_id")
    val orderId: String
)
