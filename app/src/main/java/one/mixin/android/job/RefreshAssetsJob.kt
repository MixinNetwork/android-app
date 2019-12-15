package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Fiats

class RefreshAssetsJob(private val assetId: String? = null) : MixinJob(Params(PRIORITY_UI_HIGH)
    .addTags(GROUP).singleInstanceBy(assetId ?: "all-assets").persist().requireNetwork(), assetId
    ?: "all-assets") {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAssetsJob"
    }

    override fun onAdded() {
        jobManager.saveJob(this)
    }

    override fun onRun() = runBlocking {
        if (assetId != null) {
            val response = assetService.asset(assetId)
            if (response.isSuccess && response.data != null) {
                response.data?.let {
                    assetRepo.insert(it)
                }
            }
        } else {
            val response = assetService.assetsSuspend()
            if (response.isSuccess && response.data != null) {
                val list = response.data as List<Asset>
                assetRepo.insertList(list)
            }
            refreshFiats()
        }
        removeJob()
    }

    private fun refreshFiats() = runBlocking {
        val resp = accountService.getFiats()
        if (resp.isSuccess) {
            resp.data?.let { fiatList ->
                Fiats.updateFiats(fiatList)
            }
        }
    }

    override fun cancel() {
    }
}
