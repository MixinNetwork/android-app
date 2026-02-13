package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.session.Session
import timber.log.Timber

class RefreshPerpsPositionsJob(
    private val walletId: String? = null
) : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshPerpsPositionsJob"
    }

    override fun onRun(): Unit = runBlocking {
        val perpsDb = PerpsDatabase.getDatabase(applicationContext)
        val positionDao = perpsDb.perpsPositionDao()
        val marketDao = perpsDb.perpsMarketDao()

        if (walletId != null) {
            refreshPositions(walletId, positionDao, marketDao)
        } else {
            val wallets = web3WalletDao.getAllWallets().filter { !it.isWatch() }.map { it.id }.toMutableSet()
            Session.getAccountId()?.let { wallets.add(it) }

            wallets.forEach { wId ->
                refreshPositions(wId, positionDao, marketDao)
            }
        }
    }

    private suspend fun refreshPositions(
        walletId: String,
        positionDao: PerpsPositionDao,
        marketDao: PerpsMarketDao
    ) {
        try {
            val response = routeService.getPerpsPositions(walletId = walletId)

            if (response.isSuccess && response.data != null) {
                val positions = response.data!!
                Timber.d("RefreshPerpsPositionsJob: Fetched ${positions.size} positions for wallet $walletId")
                
                if (positions.isNotEmpty()) {
                    positionDao.insertAll(positions)
                    
                    val marketIds = positions.map { it.marketId }.distinct()
                    if (marketIds.isNotEmpty()) {
                        refreshMarkets(marketIds, marketDao)
                    }
                }
            } else {
                Timber.e("RefreshPerpsPositionsJob: Failed to fetch positions for wallet $walletId: ${response.errorDescription}")
            }
        } catch (e: Exception) {
            Timber.e(e, "RefreshPerpsPositionsJob: Exception occurred while fetching positions for wallet $walletId")
        }
    }

    private suspend fun refreshMarkets(
        marketIds: List<String>,
        marketDao: PerpsMarketDao
    ) {
        marketIds.forEach { marketId ->
            try {
                if (marketDao.getMarket(marketId) == null) {
                    val response = routeService.getPerpsMarket(marketId)
                    if (response.isSuccess && response.data != null) {
                        marketDao.insert(response.data!!)
                        Timber.d("RefreshPerpsPositionsJob: Successfully inserted market $marketId")
                    } else {
                        Timber.e("RefreshPerpsPositionsJob: Failed to fetch market $marketId: ${response.errorDescription}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "RefreshPerpsPositionsJob: Exception occurred while fetching market $marketId")
            }
        }
    }
}
