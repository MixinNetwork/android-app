package one.mixin.android.vo

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

data class TokenEntry(
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("balance")
    @ColumnInfo(name = "balance")
    val balance: String,
    @SerializedName("chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @SerializedName("symbol")
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
)