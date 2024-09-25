package one.mixin.android.ui.wallet.alert.vo

import androidx.room.ColumnInfo

class CoinItem(
    @ColumnInfo("coin_id") val coinId: String, @ColumnInfo("icon_url") val iconUrl: String, @ColumnInfo("symbol") val symbol: String, @ColumnInfo("price_usd") val priceUsd: String
)