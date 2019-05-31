package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.Asset

class RefreshAssetsJob(private val assetId: String? = null) : MixinJob(Params(PRIORITY_UI_HIGH)
    .addTags(GROUP).persist().requireNetwork(), assetId ?: "all-assets") {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAssetsJob"
    }

    override fun onAdded() {
        jobManager.saveJob(this)
    }

    override fun onRun() {
        if (assetId != null) {
            val response = assetService.asset(assetId).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                response.data?.let {
                    assetRepo.upsert(it)
                }
            }
        } else {
            val response = assetService.assets().execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val list = response.data as List<Asset>
                for (item in list) {
                    assetRepo.upsert(item)
                }
            }
        }
        removeJob()
    }
    override fun cancel() {
    }
}