package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.market.MarketCoin

class RefreshMarketJob(private val id: String, private val isCoinId: Boolean) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketJob"
    }

    override fun onRun() = runBlocking {
        val response = routeService.market(id) // asset id or coin id
        if (response.isSuccess && response.data != null) {
            response.data?.let { market ->
                marketDao.insert(market)
                marketCoinDao.insertList(market.assetIds?.map { assetId ->
                    MarketCoin(
                        coinId = market.coinId,
                        assetId = assetId,
                        createdAt = nowInUtc()
                    )
                } ?: emptyList())
                if (market.assetIds.isNullOrEmpty()) {
                    marketCoinDao.deleteByCoinId(market.coinId)
                } else {
                    marketCoinDao.deleteMarketCoinsNotInAssetIds(market.coinId, market.assetIds)
                }
            }
        } else if (response.errorCode == 404 && isCoinId) {
            marketCoinDao.deleteByCoinId(id)
            marketFavoredDao.deleteByCoinId(id)
            marketDao.deleteByCoinId(id)
        }
    }
}
