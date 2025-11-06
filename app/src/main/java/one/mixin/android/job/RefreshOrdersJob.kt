package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.event.BadgeEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.session.Session

class RefreshOrdersJob : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshOrdersJob"
        const val LIMIT = 20
    }

    override fun onRun(): Unit =
        runBlocking {
            val lastCreate = orderDao.lastOrderCreatedAt()
            refreshOrders(lastCreate)
        }

    private suspend fun refreshOrders(offset: String?) {
        val response = routeService.getLimitOrders(offset = offset, limit = LIMIT)
        if (response.isSuccess && response.data != null) {
            val orders = response.data!!
            orderDao.insertListSuspend(orders)
            val walletId = Session.getAccountId()!!
            val assetIds = orders.flatMap { listOfNotNull(it.payAssetId, it.receiveAssetId) }
                .filter { it.isNotEmpty() }
                .toSet()
                .toList()
            if (assetIds.isNotEmpty()) {
                val existing = web3TokenDao.findWeb3TokenItemsByIdsSync(walletId, assetIds)
                val existingIds = existing.map { it.assetId }.toSet()
                val missingIds = assetIds.filter { it !in existingIds }
                if (missingIds.isNotEmpty()) {
                    refreshAsset(missingIds)
                }
            }
            if (response.data!!.size >= LIMIT) {
                val lastCreate = response.data?.last()?.createdAt ?: return
                refreshOrders(lastCreate)
            }
        }
    }

    private suspend fun refreshAsset(ids: List<String>) {
        val walletId = Session.getAccountId()!!
        val resp = tokenService.fetchTokenSuspend(ids)
        val tokens = resp.data ?: return
        val web3Tokens = tokens.map { t ->
            Web3Token(
                walletId = walletId,
                assetId = t.assetId,
                chainId = t.chainId,
                name = t.name,
                assetKey = t.assetKey,
                symbol = t.symbol,
                iconUrl = t.iconUrl,
                precision = t.precision,
                kernelAssetId = "",
                balance = "0",
                priceUsd = t.priceUsd,
                changeUsd = t.changeUsd,
            )
        }
        if (web3Tokens.isNotEmpty()) {
            web3TokenDao.insertList(web3Tokens)
        }
    }
}
