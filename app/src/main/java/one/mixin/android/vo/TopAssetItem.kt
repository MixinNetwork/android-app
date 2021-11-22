package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@SuppressLint("ParcelCreator")
@Parcelize
@JsonClass(generateAdapter = true)
data class TopAssetItem(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @Json(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @Json(name = "icon_url")
    val iconUrl: String,
    @Json(name = "chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @Json(name = "chain_icon_url")
    @ColumnInfo(name = "chain_icon_url")
    val chainIconUrl: String?,
    val priceUsd: String,
    val changeUsd: String,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TopAssetItem>() {
            override fun areItemsTheSame(oldItem: TopAssetItem, newItem: TopAssetItem) =
                oldItem.assetId == newItem.assetId

            override fun areContentsTheSame(oldItem: TopAssetItem, newItem: TopAssetItem) =
                oldItem == newItem
        }
    }

    fun priceFiat(): BigDecimal = if (priceUsd == "0") {
        BigDecimal.ZERO
    } else BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
}
