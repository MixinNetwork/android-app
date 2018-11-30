package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.TopAssetDao
import one.mixin.android.vo.TopAsset
import javax.inject.Inject

class RefreshTopAssetsWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {

    @Inject
    lateinit var assetService: AssetService
    @Inject
    lateinit var topAssetDao: TopAssetDao

    override fun onRun(): Result {
        val response = assetService.topAssets().execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val assetList = response.data as List<TopAsset>
            topAssetDao.insertList(assetList)
            Result.SUCCESS
        } else {
            Result.FAILURE
        }
    }
}