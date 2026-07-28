package one.mixin.android.vo.market

enum class MarketCategory(
    val value: Int,
    val apiValue: String,
) {
    TRENDING(1, "trending"),
    TOP_GAINER(2, "top_gainers"),
    TOP_LOSER(3, "top_losers"),
    STOCK(4, "stocks"),
    ;

    companion object {
        fun fromApiValue(apiValue: String): MarketCategory? =
            entries.firstOrNull { it.apiValue == apiValue }
    }
}
