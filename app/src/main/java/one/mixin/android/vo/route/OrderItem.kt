package one.mixin.android.vo.route

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    @ColumnInfo(name = "order_id")
    val orderId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "pay_asset_id")
    val payAssetId: String,
    @ColumnInfo(name = "asset_icon_url")
    val assetIconUrl: String?,
    @ColumnInfo(name = "asset_symbol")
    val assetSymbol: String?,
    @ColumnInfo(name = "receive_asset_id")
    val receiveAssetId: String,
    @ColumnInfo(name = "receive_asset_icon_url")
    val receiveAssetIconUrl: String?,
    @ColumnInfo(name = "receive_asset_symbol")
    val receiveAssetSymbol: String?,
    @ColumnInfo(name = "pay_amount")
    val payAmount: String,
    @ColumnInfo(name = "receive_amount")
    val receiveAmount: String?,
    @ColumnInfo(name = "pay_trace_id")
    val payTraceId: String?,
    @ColumnInfo(name = "receive_trace_id")
    val receiveTraceId: String?,
    @ColumnInfo(name = "state")
    val state: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "order_type")
    val orderType: String,
    @ColumnInfo(name = "fund_status")
    val fundStatus: String? = null,
    @ColumnInfo(name = "price")
    val price: String? = null,
    @ColumnInfo(name = "pending_amount")
    val pendingAmount: String? = null,
    @ColumnInfo(name = "filled_receive_amount")
    val filledReceiveAmount: String? = null,
    @ColumnInfo(name = "expected_receive_amount")
    val expectedReceiveAmount: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
    @ColumnInfo(name = "expired_at")
    val expiredAt: String? = null,
) : Parcelable
