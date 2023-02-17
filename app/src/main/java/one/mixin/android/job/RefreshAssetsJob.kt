package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Fiats

class RefreshAssetsJob(
    private val assetId: String? = null,
) : MixinJob(
    Params(PRIORITY_UI_HIGH)
        .singleInstanceBy(assetId ?: "all-assets").persist().requireNetwork(),
    assetId ?: "all-assets",
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAssetsJob"
    }

    override fun onRun() = runBlocking {
        if (assetId != null) {
            val response = assetService.getAssetByIdSuspend(assetId)
            if (response.isSuccess && response.data != null) {
                response.data?.let {
                    assetRepo.insert(it)
                    refreshChainById(it.chainId)
                }
            }
        } else {
            val response = assetService.fetchAllAssetSuspend()
            if (response.isSuccess && response.data != null) {
                val list = response.data as List<Asset>
                response.data?.map {
                    it.assetId
                }?.let { ids ->
                    assetDao.findAllAssetIdSuspend().subtract(ids.toSet()).chunked(100).forEach {
                        assetDao.zeroClearSuspend(it)
                    }
                }
                assetRepo.insertList(list)
            }
            refreshChains()
            refreshFiats()
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
        val resp = assetService.getChains()
        if (resp.isSuccess) {
            resp.data?.let { chains ->
                chainDao.upsertList(chains)
            }
        }
    }

    private suspend fun refreshChainById(chainId: String) {
        val exists = chainDao.checkExistsById(chainId)
        if (exists != null) {
            return
        }

        val resp = assetService.getChainById(chainId)
        if (resp.isSuccess) {
            resp.data?.let { chain ->
                chainDao.upsert(chain)
            }
        }
    }

    override fun cancel() {
    }
}
