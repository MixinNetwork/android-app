package one.mixin.android.api.response.perps

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "perps_orders")
data class PerpsOrder(
    @PrimaryKey
    @SerializedName("order_id")
    @ColumnInfo(name = "order_id")
    val orderId: String,
    @SerializedName("position_id")
    @ColumnInfo(name = "position_id")
    val positionId: String,
    @SerializedName(value = "market_id")
    @ColumnInfo(name = "market_id")
    val marketId: String,
    @SerializedName("side")
    @ColumnInfo(name = "side")
    val side: String,
    @SerializedName("order_type")
    @ColumnInfo(name = "order_type")
    val orderType: String,
    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: String,
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    val quantity: String,
    @SerializedName("price")
    @ColumnInfo(name = "price")
    val price: String,
    @SerializedName("entry_price")
    @ColumnInfo(name = "entry_price")
    val entryPrice: String,
    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,
    @SerializedName("realized_pnl")
    @ColumnInfo(name = "realized_pnl")
    val realizedPnl: String,
    @SerializedName("close_reason")
    @ColumnInfo(name = "close_reason")
    val closeReason: String?,
    @SerializedName("trigger_price")
    @ColumnInfo(name = "trigger_price")
    val triggerPrice: String?,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
) : Parcelable {
    companion object {
        const val TYPE_OPEN = "open"
        const val TYPE_INCREASE = "increase_position"
        const val TYPE_CLOSE = "close"

        const val CLOSE_REASON_TAKE_PROFIT = "take_profit"
        const val CLOSE_REASON_STOP_LOSS = "stop_loss"
        const val CLOSE_REASON_LIQUIDATION = "liquidation"
    }
}
