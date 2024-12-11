package one.mixin.android.vo.market

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.OptionalListConverter

@Parcelize
@TypeConverters(OptionalListConverter::class)
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
    @ColumnInfo(name = "market_cap")
    val marketCap: String,
    @ColumnInfo(name = "market_cap_rank")
    val marketCapRank: String,
    @ColumnInfo(name = "total_volume")
    val totalVolume: String,
    @ColumnInfo(name = "high_24h")
    val high24h: String,
    @ColumnInfo(name = "low_24h")
    val low24h: String,
    @ColumnInfo(name = "price_change_24h")
    val priceChange24h: String,
    @ColumnInfo(name = "price_change_percentage_1h")
    val priceChangePercentage1H: String,
    @ColumnInfo(name = "price_change_percentage_24h")
    val priceChangePercentage24H: String,
    @ColumnInfo(name = "price_change_percentage_7d")
    val priceChangePercentage7D: String,
    @ColumnInfo(name = "price_change_percentage_30d")
    val priceChangePercentage30D: String,
    @ColumnInfo(name = "market_cap_change_24h")
    val marketCapChange24h: String,
    @ColumnInfo(name = "market_cap_change_percentage_24h")
    val marketCapChangePercentage24h: String,
    @ColumnInfo(name = "circulating_supply")
    val circulatingSupply: String,
    @ColumnInfo(name = "total_supply")
    val totalSupply: String,
    @ColumnInfo(name = "max_supply")
    val maxSupply: String,
    @ColumnInfo(name = "ath")
    val ath: String,
    @ColumnInfo(name = "ath_change_percentage")
    val athChangePercentage: String,
    @ColumnInfo(name = "ath_date")
    val athDate: String,
    @ColumnInfo(name = "atl")
    val atl: String,
    @ColumnInfo(name = "atl_change_percentage")
    val atlChangePercentage: String,
    @ColumnInfo(name = "atl_date")
    val atlDate: String,
    @ColumnInfo(name = "asset_ids")
    val assetIds: List<String>?,
    @ColumnInfo(name = "sparkline_in_7d")
    val sparklineIn7d: String,
    @ColumnInfo(name = "sparkline_in_24h")
    val sparklineIn24: String,
    @ColumnInfo(name = "is_favored")
    var isFavored: Boolean?
) : Parcelable {
    companion object {
        fun fromMarket(market: Market): MarketItem {
            return MarketItem(
                coinId = market.coinId,
                name = market.name,
                symbol = market.symbol,
                iconUrl = market.iconUrl,
                currentPrice = market.currentPrice,
                marketCap = market.marketCap,
                marketCapRank = market.marketCapRank,
                totalVolume = market.totalVolume,
                high24h = market.high24h,
                low24h = market.low24h,
                priceChange24h = market.priceChange24h,
                priceChangePercentage1H = market.priceChangePercentage1H,
                priceChangePercentage24H = market.priceChangePercentage24H,
                priceChangePercentage7D = market.priceChangePercentage7D,
                priceChangePercentage30D = market.priceChangePercentage30D,
                marketCapChange24h = market.marketCapChange24h,
                marketCapChangePercentage24h = market.marketCapChangePercentage24h,
                circulatingSupply = market.circulatingSupply,
                totalSupply = market.totalSupply,
                maxSupply = market.maxSupply,
                ath = market.ath,
                athChangePercentage = market.athChangePercentage,
                athDate = market.athDate,
                atl = market.atl,
                atlChangePercentage = market.atlChangePercentage,
                atlDate = market.atlDate,
                assetIds = market.assetIds,
                sparklineIn7d = market.sparklineIn7d,
                sparklineIn24 = market.sparklineIn24h,
                isFavored = null
            )
        }
    }
}