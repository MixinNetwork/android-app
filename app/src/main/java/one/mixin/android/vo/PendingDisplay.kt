package one.mixin.android.vo

import androidx.room.ColumnInfo

class PendingDisplay(
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "amount")
    val amount: String,
    @ColumnInfo(name = "asset_id")
    val assetId: String
)