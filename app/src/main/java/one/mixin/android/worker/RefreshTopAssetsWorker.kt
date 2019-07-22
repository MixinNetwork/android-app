package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.TopAssetDao
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.vo.TopAsset

class RefreshTopAssetsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val topAssetDao: TopAssetDao
) : BaseWork(context, parameters) {

    override fun onRun(): Result {
        val response = assetService.topAssets().execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val assetList = response.data as List<TopAsset>
            topAssetDao.insertList(assetList)
            Result.success()
        } else {
            Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}
