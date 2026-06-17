package one.mixin.android.api.response.perps

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class PerpsOrderItem(
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
    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    val quantity: String,
    @SerializedName("pay_amount")
    @ColumnInfo(name = "pay_amount")
    val payAmount: String,
    @SerializedName("entry_price")
    @ColumnInfo(name = "entry_price")
    val entryPrice: String,
    @SerializedName("close_price")
    @ColumnInfo(name = "close_price")
    val closePrice: String,
    @SerializedName("realized_pnl")
    @ColumnInfo(name = "realized_pnl")
    val realizedPnl: String,
    @SerializedName("roe")
    @ColumnInfo(name = "roe")
    val roe: String,
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
    @ColumnInfo(name = "display_symbol")
    val displaySymbol: String? = null,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    @ColumnInfo(name = "token_symbol")
    val tokenSymbol: String? = null,
    @ColumnInfo(name = "price_scale")
    val priceScale: Int = 2,
) : Parcelable
