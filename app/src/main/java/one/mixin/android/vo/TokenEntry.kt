package one.mixin.android.vo

import androidx.room.ColumnInfo
import kotlinx.serialization.SerialName

data class TokenEntry(
    @SerialName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerialName("balance")
    @ColumnInfo(name = "balance")
    val balance: String,
    @SerialName("chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @SerialName("symbol")
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @SerialName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerialName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
)