package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.TopAssetDao
import one.mixin.android.vo.TopAsset

@HiltWorker
class RefreshTopAssetsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val topAssetDao: TopAssetDao
) : BaseWork(context, parameters) {

    override suspend fun onRun(): Result {
        val response = assetService.topAssets().execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val assetList = response.data as List<TopAsset>
            topAssetDao.insertList(assetList)
            Result.success()
        } else {
            Result.failure()
        }
    }
}
