package one.mixin.android.ui.wallet.alert.vo

import androidx.room.ColumnInfo

class AlertGroup(
    @ColumnInfo("coin_id")
    val coinId: String,
    @ColumnInfo("icon_url")
    val iconUrl: String,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("price_usd")
    val priceUsd: String
)
