package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.property.Web3PropertyHelper
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.session.Session
import timber.log.Timber

class RefreshOrdersJob(
    private val walletId: String? = null
) : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshOrdersJob"
        const val LIMIT = 20
    }

    override fun onRun(): Unit =
        runBlocking {
            if (walletId != null) {
                // Refresh specific wallet
                val offsetKey = "order_offset_$walletId"
                val offset = Web3PropertyHelper.findValueByKey(offsetKey, "")
                refreshOrders(walletId, offset.ifEmpty { null }, offsetKey)
            } else {
                // Refresh all wallets
                val wallets = web3WalletDao.getAllWallets().filter { it.isWatch().not() }.map { it.id }.toMutableSet()
                Session.getAccountId()?.let { wallets.add(it) }

                wallets.forEach { wId ->
                    val offsetKey = "order_offset_$wId"
                    val offset = Web3PropertyHelper.findValueByKey(offsetKey, "")
                    refreshOrders(wId, offset.ifEmpty { null }, offsetKey)
                }
            }
        }

    private suspend fun refreshOrders(walletId: String, offset: String?, offsetKey: String) {
        val response = routeService.getLimitOrders(category = "all", limit = LIMIT, offset = offset, state = null, walletId = walletId)

        if (response.isSuccess && response.data != null) {
            val orders = response.data!!
            if (orders.isEmpty()) return
            
            // Check if we're stuck in a loop (same last timestamp)
            val lastCreate = orders.maxByOrNull { it.createdAt }?.createdAt ?: return
            if (offset != null && lastCreate == offset) {
                Timber.w("RefreshOrdersJob: Detected duplicate offset for wallet $walletId, stopping pagination")
                return
            }
            
            orderDao.insertListSuspend(orders)
            val sessionWalletId = Session.getAccountId() ?: return
            val assetIds = orders.flatMap { listOfNotNull<String>(it.payAssetId, it.receiveAssetId) }
                .filter { it.isNotEmpty() }
                .toSet()
                .toList()
            if (assetIds.isNotEmpty()) {
                val tokensNow = web3TokenDao.findWeb3TokenItemsByIdsSync(sessionWalletId, assetIds)
                val existingIds = tokensNow.map { it.assetId }.toSet()
                val missingIds = assetIds.filter { it !in existingIds }
                if (missingIds.isNotEmpty()) {
                    refreshAsset(missingIds)
                }
                // refresh tokens again after potential insert to make sure we have chainIds
                val tokensReady = web3TokenDao.findWeb3TokenItemsByIdsSync(sessionWalletId, assetIds)
                val chainIds = tokensReady.map { it.chainId }.distinct()
                if (chainIds.isNotEmpty()) {
                    fetchChain(chainIds)
                }
            }

            val fetchedSize = orders.size
            Web3PropertyHelper.updateKeyValue(offsetKey, lastCreate)
            if (fetchedSize >= LIMIT) {
                refreshOrders(walletId, lastCreate, offsetKey)
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
