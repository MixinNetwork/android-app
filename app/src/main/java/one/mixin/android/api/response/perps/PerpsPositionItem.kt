package one.mixin.android.api.response.perps

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PerpsPositionItem(
    @SerializedName("position_id")
    @ColumnInfo(name = "position_id")
    val positionId: String,
    @SerializedName("product_id")
    @ColumnInfo(name = "product_id")
    val productId: String,
    @SerializedName("side")
    @ColumnInfo(name = "side")
    val side: String,
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    val quantity: String,
    @SerializedName("entry_price")
    @ColumnInfo(name = "entry_price")
    val entryPrice: String,
    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,
    @SerializedName("settle_asset_id")
    @ColumnInfo(name = "settle_asset_id")
    val settleAssetId: String? = null,
    @SerializedName("bot_id")
    @ColumnInfo(name = "bot_id")
    val botId: String? = null,
    @SerializedName("margin")
    @ColumnInfo(name = "margin")
    val margin: String? = null,
    @SerializedName("state")
    @ColumnInfo(name = "state")
    val state: String? = null,
    @SerializedName("mark_price")
    @ColumnInfo(name = "mark_price")
    val markPrice: String? = null,
    @SerializedName("unrealized_pnl")
    @ColumnInfo(name = "unrealized_pnl")
    val unrealizedPnl: String? = null,
    @SerializedName("roe")
    @ColumnInfo(name = "roe")
    val roe: String? = null,
    @ColumnInfo(name = "wallet_id")
    val walletId: String? = null,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
    @ColumnInfo(name = "display_symbol")
    val displaySymbol: String? = null,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    @ColumnInfo(name = "token_symbol")
    val tokenSymbol: String? = null,
) : Parcelable

fun PerpsPositionItem.toPosotion(): PerpsPosition {
    return PerpsPosition(
        positionId,
        productId,
        side,
        quantity,
        entryPrice,
        leverage,
        settleAssetId,
        botId,
        margin,
        state,
        markPrice,
        unrealizedPnl,
        roe, walletId, createdAt, updatedAt, displaySymbol, iconUrl, tokenSymbol,
    )
}