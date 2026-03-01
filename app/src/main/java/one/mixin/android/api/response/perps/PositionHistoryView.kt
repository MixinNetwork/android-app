package one.mixin.android.api.response.perps

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PositionHistoryView(
    @SerializedName("history_id")
    val historyId: String,
    @SerializedName("position_id")
    val positionId: String,
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("market_symbol")
    val marketSymbol: String? = null,
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
    @SerializedName("margin_method")
    val marginMethod: String? = null,
    @SerializedName("open_at")
    val openAt: String,
    @SerializedName("closed_at")
    val closedAt: String,
    var displaySymbol: String? = null,
    var iconUrl: String? = null,
    var tokenSymbol: String? = null
) : Parcelable