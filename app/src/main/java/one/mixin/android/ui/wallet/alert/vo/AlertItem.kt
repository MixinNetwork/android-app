package one.mixin.android.ui.wallet.alert.vo

import androidx.room.ColumnInfo

class AlertItem(
    @ColumnInfo("asset_id")
    val assetId: String,
    @ColumnInfo("icon_url")
    val iconUrl: String,
    @ColumnInfo("symbol")
    val symbol: String,
    @ColumnInfo(name = "type")
    val type: AlertType,
    @ColumnInfo(name = "frequency")
    val frequency: AlertFrequency,
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
