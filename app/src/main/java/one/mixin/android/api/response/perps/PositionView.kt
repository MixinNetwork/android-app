package one.mixin.android.api.response.perps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "perps_positions")
data class PerpsPosition(
    @PrimaryKey
    @SerializedName("position_id")
    @ColumnInfo(name = "position_id")
    val positionId: String,
    @SerializedName("market_symbol")
    @ColumnInfo(name = "market_symbol")
    val marketSymbol: String,
    @SerializedName("side")
    @ColumnInfo(name = "side")
    val side: String,
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    val quantity: String,
    @SerializedName("entry_price")
    @ColumnInfo(name = "entry_price")
    val entryPrice: String,
    @SerializedName("margin")
    @ColumnInfo(name = "margin")
    val margin: String,
    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,
    @SerializedName("state")
    @ColumnInfo(name = "state")
    val state: String,
    @SerializedName("mark_price")
    @ColumnInfo(name = "mark_price")
    val markPrice: String,
    @SerializedName("unrealized_pnl")
    @ColumnInfo(name = "unrealized_pnl")
    val unrealizedPnl: String,
    @SerializedName("roe")
    @ColumnInfo(name = "roe")
    val roe: String,
    @ColumnInfo(name = "wallet_id")
    val walletId: String = "",
    @ColumnInfo(name = "market_id")
    val marketId: String = "",
    @ColumnInfo(name = "liquidation_price")
    val liquidationPrice: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = ""
)

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
