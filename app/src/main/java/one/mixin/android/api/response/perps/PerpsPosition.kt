package one.mixin.android.api.response.perps

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "positions")
data class PerpsPosition(
    @PrimaryKey
    @SerializedName("position_id")
    @ColumnInfo(name = "position_id")
    val positionId: String,
    @SerializedName(value = "market_id")
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
    @SerializedName("settle_asset_id")
    @ColumnInfo(name = "settle_asset_id")
    val settleAssetId: String,
    @SerializedName("open_pay_amount")
    @ColumnInfo(name = "open_pay_amount")
    val openPayAmount: String,
    @SerializedName("open_pay_asset_id")
    @ColumnInfo(name = "open_pay_asset_id")
    val openPayAssetId: String,
    @SerializedName("bot_id")
    @ColumnInfo(name = "bot_id")
    val botId: String,
    @ColumnInfo(name = "wallet_id")
    val walletId: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
) : Parcelable
