package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.event.BadgeEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.session.Session
import timber.log.Timber

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
                val tokensNow = web3TokenDao.findWeb3TokenItemsByIdsSync(walletId, assetIds)
                val existingIds = tokensNow.map { it.assetId }.toSet()
                val missingIds = assetIds.filter { it !in existingIds }
                if (missingIds.isNotEmpty()) {
                    refreshAsset(missingIds)
                }
                // refresh tokens again after potential insert to make sure we have chainIds
                val tokensReady = web3TokenDao.findWeb3TokenItemsByIdsSync(walletId, assetIds)
                val chainIds = tokensReady.mapNotNull { it.chainId }.distinct()
                if (chainIds.isNotEmpty()) {
                    fetchChain(chainIds)
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


    private suspend fun fetchChain(chainIds: List<String>) {
        chainIds.forEach { chainId ->
            try {
                if (web3ChainDao.chainExists(chainId) == null) {
                    val response = tokenService.getChainById(chainId)
                    if (response.isSuccess) {
                        val chain = response.data
                        if (chain != null) {
                            web3ChainDao.insert(
                                Web3Chain(
                                    chainId = chain.chainId,
                                    name = chain.name,
                                    symbol = chain.symbol,
                                    iconUrl = chain.iconUrl,
                                    threshold = chain.threshold,
                                )
                            )
                            Timber.d("Successfully inserted ${chain.name} chain into database")
                        } else {
                            Timber.d("No chain found for chainId: $chainId")
                        }
                    } else {
                        Timber.e("Failed to fetch chain $chainId: ${response.errorCode} - ${response.errorDescription}")
                    }
                } else {
                    Timber.d("Chain $chainId already exists in local database, skipping fetch")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception occurred while fetching chain $chainId")
            }
        }
    }
}
