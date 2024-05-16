package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@SuppressLint("ParcelCreator")
@Parcelize
data class TopAssetItem(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @SerializedName("chain_icon_url")
    @ColumnInfo(name = "chain_icon_url")
    val chainIconUrl: String?,
    @SerializedName("asset_key")
    @ColumnInfo(name = "asset_key")
    val assetKey: String?,
    val priceUsd: String,
    val changeUsd: String,
    @SerializedName("collection_hash")
    @ColumnInfo(name = "collection_hash")
    val collectionHash: String?,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<TopAssetItem>() {
                override fun areItemsTheSame(
                    oldItem: TopAssetItem,
                    newItem: TopAssetItem,
                ) =
                    oldItem.assetId == newItem.assetId

                override fun areContentsTheSame(
                    oldItem: TopAssetItem,
                    newItem: TopAssetItem,
                ) =
                    oldItem == newItem
            }
    }

    fun priceFiat(): BigDecimal =
        if (priceUsd == "0") {
            BigDecimal.ZERO
        } else {
            BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
        }
}
