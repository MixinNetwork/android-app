package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.market.MarketCoin

class RefreshMarketJob(private val id: String, private val isCoinId: Boolean) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketJob"
    }

    override fun onRun(): Unit = runBlocking {
        val response = routeService.market(id)
        if (response.isSuccess && response.data != null) {
            response.data?.let { market ->
                marketDao.insert(market)
                marketCoinDao.insertList(market.assetIds?.map { assetId ->
                    MarketCoin(
                        coinId = market.coinId, assetId = assetId, createdAt = nowInUtc()
                    )
                } ?: emptyList())

                if (isCoinId.not()) { // id is token id
                    marketCoinDao.findCoinIdByTokenId(id)?.let { localCoinId ->
                        if (localCoinId != market.coinId) { // Clean up old data when coin ID changes
                            marketCoinDao.deleteByCoinId(localCoinId)
                            marketDao.deleteByCoinId(localCoinId)
                            marketFavoredDao.deleteByCoinId(localCoinId)
                        }
                    }
                }

                if (market.assetIds.isNullOrEmpty()) {
                    // Remove coin relationships when no assets are mapped
                    marketCoinDao.deleteByCoinId(market.coinId)
                } else {
                    // Remove obsolete asset mappings
                    marketCoinDao.deleteMarketCoinsNotInAssetIds(market.coinId, market.assetIds)
                }
            }
        } else if (response.errorCode == 404 && isCoinId) {
            // Clean up all related data when market is not found
            marketCoinDao.deleteByCoinId(id)
            marketFavoredDao.deleteByCoinId(id)
            marketDao.deleteByCoinId(id)
        }
    }
}
