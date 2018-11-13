package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.Asset
import one.mixin.android.vo.HotAsset
import one.mixin.android.vo.toHotAsset

class RefreshHotAssetsJob() : BaseJob(Params(PRIORITY_UI_HIGH)
    .addTags(RefreshHotAssetsJob.GROUP).persist().requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshHotAssetsJob"
    }

    override fun onRun() {
        val response = assetService.topAssets().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val assetList = response.data as List<Asset>
            val hotAssetList = arrayListOf<HotAsset>()
            assetList.mapTo(hotAssetList, { asset ->
                val chainIconUrl = assetDao.getIconUrl(asset.chainId)
                asset.toHotAsset(chainIconUrl)
            })
            hotAssetDao.insertList(hotAssetList)
        }
    }
}