package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.Token
import timber.log.Timber

class RefreshTokensJob(
    private val assetId: String? = null,
    private val conversationId: String? = null,
    private val messageId: String? = null,
) : MixinJob(
        Params(PRIORITY_UI_HIGH)
            .singleInstanceBy(assetId ?: "all-tokens").persist().requireNetwork(),
        assetId ?: "all-tokens",
    ) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshTokensJob"
    }

    override fun onRun() =
        runBlocking {
            if (!assetId.isNullOrEmpty()) {
                val response = tokenService.getAssetByIdSuspend(assetId)
                if (response.isSuccess && response.data != null) {
                    response.data?.let {
                        assetRepo.insert(it)
                        refreshChainById(it.chainId, it.chainId == it.assetId)
                    }
                    if (conversationId != null && messageId != null) {
                        MessageFlow.update(conversationId, messageId)
                    }
                }
            } else {
                fetchChains()
                refreshAsset()
                val tokenIds = tokenDao.findAllTokenIds()
                val response = tokenService.fetchTokenSuspend(tokenIds)
                if (response.isSuccess && response.data != null) {
                    val list = response.data as List<Token>
                    assetRepo.insertList(list)
                    refreshChains(list.map { it.chainId }.distinct())
                }
                refreshFiats()
            }
        }

    private suspend fun refreshAsset() {
        val response = tokenService.fetchAllTokenSuspend()
        if (response.isSuccess && response.data != null) {
            val list = response.data as List<Token>
            assetRepo.insertList(list)
        }
    }

    private suspend fun fetchChains() {
        try {
            val response = tokenService.getChains()
            if (response.isSuccess) {
                val chains = response.data
                if (chains != null && chains.isNotEmpty()) {
                    Timber.d("Fetched ${chains.size} chains")
                    chainDao.insertList(chains)
                    Timber.d("Successfully inserted ${chains.size} chains into database")
                } else {
                    Timber.d("No chains found")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception occurred while fetching chains")
        }
    }

    private suspend fun refreshFiats() {
        val resp = accountService.getFiats()
        if (resp.isSuccess) {
            resp.data?.let { fiatList ->
                Fiats.updateFiats(fiatList)
            }
        }
    }

    private suspend fun refreshChains(chainIds: List<String>) {
        chainIds.forEach { chainId ->
            refreshChainById(chainId)
        }
    }
    private suspend fun refreshChainById(chainId: String, force: Boolean = false) {
        if (!force && chainDao.checkExistsById(chainId) != null) return
        val resp = tokenService.getChainById(chainId)
        if (resp.isSuccess) {
            resp.data?.let { chain ->
                val isExits = chainDao.isExits(chain.chainId, chain.name, chain.symbol, chain.iconUrl, chain.threshold) != null
                if (!isExits) chainDao.upsertSuspend(chain)
            }
        }
    }

    override fun cancel() {
    }
}
