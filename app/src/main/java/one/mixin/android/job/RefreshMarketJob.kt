package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.market.MarketCoin
import timber.log.Timber

class RefreshMarketJob(private val id: String, private val isCoinId: Boolean = false) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketJob"
    }

    override fun onRun(): Unit = runBlocking {
        val response = routeService.market(id)
        if (response.isSuccess && response.data != null) {
            response.data?.let { market ->
                val localCoinId = if (isCoinId.not()) {
                    marketCoinDao.findCoinIdByTokenId(id)
                } else {
                    null
                }
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

                if (localCoinId != null && localCoinId != market.coinId) {
                    marketCoinDao.deleteByCoinId(localCoinId)
                    marketDao.deleteByCoinId(localCoinId)
                    marketFavoredDao.deleteByCoinId(localCoinId)
                }
            }
        } else if (response.errorCode == 404 && isCoinId) {
            marketCoinDao.deleteByCoinId(id)
            marketFavoredDao.deleteByCoinId(id)
            marketDao.deleteByCoinId(id)
        }
    }
}
