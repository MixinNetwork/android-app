package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.withRoomTransaction
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.session.Session
import one.mixin.android.session.resolveCurrentUserScopeManager
import timber.log.Timber

class RefreshPerpsPositionsJob(
    private val walletId: String? = null
) : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshPerpsPositionsJob"
    }

    override fun onRun(): Unit = runBlocking {
        val perpsDb = runCatching { resolveCurrentUserScopeManager(applicationContext).getPerpsDatabase() }
            .getOrElse {
                Timber.w(it, "RefreshPerpsPositionsJob: Skip because perps database scope is unavailable")
                return@runBlocking
            }
        val positionDao = perpsDb.perpsPositionDao()

        if (walletId != null) {
            refreshPositions(walletId, positionDao)
        } else {
            val wallets = web3WalletDao.getAllWallets().filter { !it.isWatch() }.map { it.id }.toMutableSet()
            Session.getAccountId()?.let { wallets.add(it) }

            wallets.forEach { wId ->
                refreshPositions(wId, positionDao)
            }
        }
    }

    private suspend fun refreshPositions(
        walletId: String,
        positionDao: PerpsPositionDao,
    ) {
        try {
            val response = routeService.getPerpsPositions(walletId = walletId)

            if (response.isSuccess && response.data != null) {
                val positions = response.data!!.map { it.copy(walletId = walletId) }
                Timber.d("RefreshPerpsPositionsJob: Fetched ${positions.size} positions for wallet $walletId")

                val perpsDb = resolveCurrentUserScopeManager(applicationContext).getPerpsDatabase()
                perpsDb.withRoomTransaction {
                    if (positions.isEmpty()) {
                        positionDao.deleteOpenByWallet(walletId)
                    } else {
                        val positionIds = positions.map { it.positionId }
                        positionDao.deleteOpenByWalletAndNotIn(walletId, positionIds)
                        positionDao.insertAll(positions)
                    }
                }
            } else {
                Timber.e("RefreshPerpsPositionsJob: Failed to fetch positions for wallet $walletId: ${response.errorDescription}")
            }
        } catch (e: Exception) {
            Timber.e(e, "RefreshPerpsPositionsJob: Exception occurred while fetching positions for wallet $walletId")
        }
    }
}
