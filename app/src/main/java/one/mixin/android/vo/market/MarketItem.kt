package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import one.mixin.android.vo.ListConverter

@TypeConverters(ListConverter::class)
data class MarketItem(
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "current_price")
    val currentPrice: String,
    @ColumnInfo(name = "price_change_percentage_24h")
    val priceChangePercentage24h: String,
    @ColumnInfo(name = "market_cap")
    val marketCap: String,
    @ColumnInfo(name = "market_cap_rank")
    val marketCapRank: String,
    @ColumnInfo(name = "total_volume")
    val totalVolume: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "asset_ids")
    val assetIds: List<String>,
    @ColumnInfo(name = "sparkline_in_7d")
    val sparklineIn7d: String,
    @ColumnInfo(name = "is_favored")
    var isFavored: Boolean?
)