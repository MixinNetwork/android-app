package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.Token

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
                        refreshChainById(it.chainId)
                    }
                    if (conversationId != null && messageId != null) {
                        MessageFlow.update(conversationId, messageId)
                    }
                }
            } else {
                refreshAsset()
                val tokenIds = tokenDao.findAllTokenIds()
                val response = tokenService.fetchTokenSuspend(tokenIds)
                if (response.isSuccess && response.data != null) {
                    val list = response.data as List<Token>
                    assetRepo.insertList(list)
                }
                refreshChains()
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

    private suspend fun refreshFiats() {
        val resp = accountService.getFiats()
        if (resp.isSuccess) {
            resp.data?.let { fiatList ->
                Fiats.updateFiats(fiatList)
            }
        }
    }

    private suspend fun refreshChains() {
        val resp = tokenService.getChains()
        if (resp.isSuccess) {
            resp.data?.let { chains ->
                chains.subtract(chainDao.getChains().toSet()).let {
                    chainDao.insertList(it.toList())
                }
            }
        }
    }

    private suspend fun refreshChainById(chainId: String) {
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
