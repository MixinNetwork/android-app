package one.mixin.android.vo.route

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwapOrderItem(
    @ColumnInfo(name = "order_id")
    @SerializedName("order_id")
    val orderId: String,
    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    val userId: String,
    @ColumnInfo(name = "pay_asset_id")
    @SerializedName("pay_asset_id")
    val payAssetId: String,
    @ColumnInfo(name = "pay_chain_id")
    @SerializedName("pay_chain_id")
    val payChainId: String?,
    @ColumnInfo(name = "asset_icon_url")
    @SerializedName("asset_icon_url")
    val assetIconUrl: String?,
    @ColumnInfo(name = "asset_symbol")
    @SerializedName("asset_symbol")
    val assetSymbol: String?,
    @ColumnInfo(name = "receive_asset_id")
    @SerializedName("receive_asset_id")
    val receiveAssetId: String,
    @ColumnInfo(name = "receive_chain_id")
    @SerializedName("receive_chain_id")
    val receiveChainId: String?,
    @ColumnInfo(name = "receive_asset_icon_url")
    @SerializedName("receive_asset_icon_url")
    val receiveAssetIconUrl: String?,
    @ColumnInfo(name = "receive_asset_symbol")
    @SerializedName("receive_asset_symbol")
    val receiveAssetSymbol: String?,
    @ColumnInfo(name = "pay_amount")
    @SerializedName("pay_amount")
    val payAmount: String,
    @ColumnInfo(name = "receive_amount")
    @SerializedName("receive_amount")
    val receiveAmount: String,
    @ColumnInfo(name = "state")
    @SerializedName("state")
    val state: String,
    @ColumnInfo(name = "order_type")
    @SerializedName("order_type")
    val type: String,
    @ColumnInfo(name = "pay_chain_name")
    @SerializedName("pay_chain_name")
    val payChainName: String?,
    @ColumnInfo(name = "receive_chain_name")
    @SerializedName("receive_chain_name")
    val receiveChainName: String?,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
) : Parcelable
