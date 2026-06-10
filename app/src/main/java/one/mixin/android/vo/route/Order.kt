package one.mixin.android.vo.route

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["state", "created_at"]),
        Index(value = ["order_type", "created_at"])
    ],
)
data class Order(
    @PrimaryKey
    @ColumnInfo(name = "order_id")
    @SerializedName("order_id")
    val orderId: String,
    @ColumnInfo(name = "wallet_id")
    @SerializedName("wallet_id")
    val walletId: String,
    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    val userId: String,
    @ColumnInfo(name = "pay_asset_id")
    @SerializedName("pay_asset_id")
    val payAssetId: String,
    @ColumnInfo(name = "receive_asset_id")
    @SerializedName("receive_asset_id")
    val receiveAssetId: String,
    @ColumnInfo(name = "pay_amount")
    @SerializedName("pay_amount")
    val payAmount: String,
    @ColumnInfo(name = "receive_amount")
    @SerializedName("receive_amount")
    val receiveAmount: String?,
    @ColumnInfo(name = "pay_trace_id")
    @SerializedName("pay_trace_id")
    val payTraceId: String?,
    @ColumnInfo(name = "receive_trace_id")
    @SerializedName("receive_trace_id")
    val receiveTraceId: String?,
    @ColumnInfo(name = "state")
    @SerializedName("state")
    val state: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    @ColumnInfo(name = "order_type")
    @SerializedName("order_type")
    val orderType: String, // "swap" or "limit"
    // Limit order specific
    @ColumnInfo(name = "fund_status")
    @SerializedName("fund_status")
    val fundStatus: String? = null,
    @ColumnInfo(name = "price")
    @SerializedName("price")
    val price: String? = null,
    @ColumnInfo(name = "pending_amount")
    @SerializedName("pending_amount")
    val pendingAmount: String? = null,
    @ColumnInfo(name = "filled_receive_amount")
    @SerializedName("filled_receive_amount")
    val filledReceiveAmount: String? = null,
    @ColumnInfo(name = "expected_receive_amount")
    @SerializedName("expected_receive_amount")
    val expectedReceiveAmount: String? = null,
    @ColumnInfo(name = "expired_at")
    @SerializedName("expired_at")
    val expiredAt: String? = null,
) : Parcelable

fun Order.isPending() = state == OrderState.PENDING.value

fun Order.isCancelling() = state == OrderState.CANCELLING.value
