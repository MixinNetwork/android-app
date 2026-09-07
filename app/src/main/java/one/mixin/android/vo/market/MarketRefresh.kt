package one.mixin.android.vo.market

private const val DEFAULT_MARKET_REFRESH_LIMIT = 500
private const val TOP_MOVER_MARKET_REFRESH_LIMIT = 100

internal fun marketRefreshLimit(category: MarketCategory): Int =
    when (category) {
        MarketCategory.TOP_GAINER,
        MarketCategory.TOP_LOSER,
        -> TOP_MOVER_MARKET_REFRESH_LIMIT

        else -> DEFAULT_MARKET_REFRESH_LIMIT
    }

internal sealed interface MarketRefreshResult {
    data class Success(
        val markets: List<MarketItem>,
    ) : MarketRefreshResult

    data class Failure(
        val errorCode: Int? = null,
        val errorDescription: String? = null,
    ) : MarketRefreshResult
}

internal fun Iterable<MarketRefreshResult>.hasErrorCode(errorCode: Int): Boolean =
    any { result ->
        result is MarketRefreshResult.Failure && result.errorCode == errorCode
    }
