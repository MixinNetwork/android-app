package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.api.service.AssetService
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.repository.AssetRepository
import one.mixin.android.vo.Asset

class RefreshAssetsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val assetRepo: AssetRepository
) : BaseWork(context, parameters) {
    companion object {
        const val ASSET_ID = "asset_id"
    }

    override fun onRun(): Result {
        val assetId = inputData.getString(ASSET_ID)
        if (assetId != null) {
            val response = assetService.asset(assetId).execute().body()
            return if (response != null && response.isSuccess && response.data != null) {
                response.data.let {
                    assetRepo.insert(it!!)
                }
                Result.success()
            } else {
                Result.failure()
            }
        } else {
            val response = assetService.assets().execute().body()
            return if (response != null && response.isSuccess && response.data != null) {
                val list = response.data as List<Asset>
                assetRepo.insertList(list)
                Result.success()
            } else {
                Result.failure()
            }
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}
