package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.market.MarketCoin

class RefreshMarketJob(private val assetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketJob"
    }

    override fun onRun() = runBlocking {
        val response = routeService.market(assetId)
        if (response.isSuccess && response.data != null) {
            response.data?.let {
                val local = marketDao.findMarketById(it.coinId)
                val market = if (local != null) {
                    it.copy(marketCapRank = it.marketCapRank)
                } else {
                    it
                }
                marketDao.insert(market)
                marketCoinDao.insertIgnoreList(it.assetIds?.map { assetId ->
                    MarketCoin(
                        coinId = it.coinId,
                        assetId = assetId,
                        createdAt = nowInUtc()
                    )
                } ?: emptyList())
            }
        }
    }
}
