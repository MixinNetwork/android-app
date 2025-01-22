package one.mixin.android.vo.route

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.ListConverter

@Parcelize
@TypeConverters(ListConverter::class)
@Entity(
    tableName = "swap_orders",
    indices = [Index(value = ["created_at"], orders = [Index.Order.DESC])],
)
data class SwapOrder(
    @PrimaryKey
    @ColumnInfo(name = "order_id")
    @SerializedName("order_id")
    val orderId: String,
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
    val receiveAmount: String,
    @ColumnInfo(name = "pay_trace_id")
    @SerializedName("pay_trace_id")
    val payTraceId: String,
    @ColumnInfo(name = "receive_trace_id")
    @SerializedName("receive_trace_id")
    val receiveTraceId: String,
    @ColumnInfo(name = "state")
    @SerializedName("state")
    val state: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    @ColumnInfo(name = "order_type")
    @SerializedName("order_type")
    val orderType: String
) : Parcelable
