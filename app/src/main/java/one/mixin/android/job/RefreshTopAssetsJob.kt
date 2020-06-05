package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.TopAsset

class RefreshTopAssetsJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(RefreshTopAssetsJob.GROUP).requireNetwork()
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAddressJob"
    }

    override fun onRun() {
        val response = assetService.topAssets().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val assetList = response.data as List<TopAsset>
            topAssetDao.insertList(assetList)
        }
    }
}
