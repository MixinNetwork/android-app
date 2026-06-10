package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.market.MarketCoin
import timber.log.Timber

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
            response.data?.let { market ->
                marketDao.insert(market)
                val remoteAssetIds = market.assetIds ?: emptyList()
                val localAssetIds = marketCoinDao.findTokenIdsByCoinId(market.coinId)
                val assetIdsToDelete = localAssetIds.filter { it !in remoteAssetIds }
                if (assetIdsToDelete.isNotEmpty()) {
                    Timber.e("Deleting assets for coinId: ${market.coinId}, assetIds: $assetIdsToDelete")
                    marketCoinDao.deleteByCoinIdAndAssetIds(market.coinId, assetIdsToDelete)
                }
                marketCoinDao.insertList(remoteAssetIds.map { assetId ->
                    MarketCoin(
                        coinId = market.coinId,
                        assetId = assetId,
                        createdAt = nowInUtc()
                    )
                })
            }
        }
    }
}
