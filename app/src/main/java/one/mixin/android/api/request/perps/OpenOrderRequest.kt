package one.mixin.android.api.request.perps

import com.google.gson.annotations.SerializedName

data class OpenOrderRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("market_id")
    val marketId: String,
    @SerializedName("side")
    val side: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("leverage")
    val leverage: Int,
    @SerializedName("wallet_id")
    val walletId: String,
    @SerializedName("take_profit_price")
    val takeProfitPrice: String? = null,
    @SerializedName("stop_loss_price")
    val stopLossPrice: String? = null,
    @SerializedName("destination")
    val destination: String? = null
)

data class OpenOrderResponse(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("payment_url")
    val paymentUrl: String?,
    @SerializedName("pay_amount")
    val payAmount: String,
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

data class PositionTpSlRequest(
    @SerializedName("position_id")
    val positionId: String,
    @SerializedName("take_profit_price")
    val takeProfitPrice: String? = null,
    @SerializedName("stop_loss_price")
    val stopLossPrice: String? = null,
)

data class IncreaseOrderRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("destination")
    val destination: String? = null,
    @SerializedName("price")
    val price: String? = null,
    @SerializedName("take_profit_price")
    val takeProfitPrice: String? = null,
    @SerializedName("stop_loss_price")
    val stopLossPrice: String? = null,
)
