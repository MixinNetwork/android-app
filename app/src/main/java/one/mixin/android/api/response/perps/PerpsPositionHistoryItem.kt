package one.mixin.android.api.response.perps

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PerpsPositionHistoryItem(
    @SerializedName("history_id")
    @ColumnInfo(name = "history_id")
    val historyId: String,
    @SerializedName("position_id")
    @ColumnInfo(name = "position_id")
    val positionId: String,
    @SerializedName(value = "market_id", alternate = ["product_id"])
    @ColumnInfo(name = "market_id")
    val marketId: String,
    @SerializedName("side")
    @ColumnInfo(name = "side")
    val side: String,
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    val quantity: String,
    @SerializedName("entry_price")
    @ColumnInfo(name = "entry_price")
    val entryPrice: String,
    @SerializedName("close_price")
    @ColumnInfo(name = "close_price")
    val closePrice: String,
    @SerializedName("realized_pnl")
    @ColumnInfo(name = "realized_pnl")
    val realizedPnl: String,
    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,
    @SerializedName("margin_method")
    @ColumnInfo(name = "margin_method")
    val marginMethod: String? = null,
    @SerializedName("open_at")
    @ColumnInfo(name = "open_at")
    val openAt: String,
    @SerializedName("closed_at")
    @ColumnInfo(name = "closed_at")
    val closedAt: String,
    @ColumnInfo(name = "wallet_id")
    val walletId: String,
    @ColumnInfo(name = "display_symbol")
    val displaySymbol: String? = null,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    @ColumnInfo(name = "token_symbol")
    val tokenSymbol: String? = null
) : Parcelable
