package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketCapRank
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.market.MarketFavored

class RefreshMarketsJob(val category: String = "all") : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .groupBy(GROUP).addTags(category).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketsJob"
        private const val LIMIT = 500
    }

    override fun onRun(): Unit = runBlocking {
        requestRouteAPI(
            invokeNetwork = {
                routeService.markets(category = category, limit = LIMIT)
            },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            successBlock = { response ->
                val list = response.data!!
                val now = nowInUtc()
                if (category == "favorite") {
                    val marketExtraList: List<MarketFavored> = list.map { market ->
                        MarketFavored(
                            coinId = market.coinId,
                            isFavored = true,
                            now
                        )
                    }
                    marketFavoredDao.insertList(marketExtraList)
                }
                if (category == "all") {
                    marketCapRankDao.insertAll(list.map {
                        MarketCapRank(it.coinId, it.marketCapRank, it.updatedAt)
                    })
                }
                marketDao.upsertList(list)
                val ids = list.flatMap { market ->
                    market.assetIds?.map { assetId ->
                        MarketCoin(
                            coinId = market.coinId,
                            assetId = assetId,
                            createdAt = now
                        )
                    } ?: emptyList()
                }
                marketCoinDao.insertIgnoreList(ids)
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            }
        )
    }
}
