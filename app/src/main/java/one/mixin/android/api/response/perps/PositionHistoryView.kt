package one.mixin.android.api.response.perps

import com.google.gson.annotations.SerializedName

data class PositionHistoryView(
    @SerializedName("history_id")
    val historyId: String,
    @SerializedName("position_id")
    val positionId: String,
    @SerializedName("market_symbol")
    val marketSymbol: String,
    @SerializedName("side")
    val side: String,
    @SerializedName("quantity")
    val quantity: String,
    @SerializedName("entry_price")
    val entryPrice: String,
    @SerializedName("close_price")
    val closePrice: String,
    @SerializedName("realized_pnl")
    val realizedPnl: String,
    @SerializedName("leverage")
    val leverage: Int,
    @SerializedName("open_at")
    val openAt: String,
    @SerializedName("closed_at")
    val closedAt: String
)