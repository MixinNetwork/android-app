package one.mixin.android.api.response.perps

enum class PerpsMarketCategory(
    val value: Int,
    val apiValue: String,
) {
    TRENDING(1, "trending"),
    TOP_GAINER(2, "top_gainer"),
    TOP_LOSER(3, "top_loser"),
    ;
}
