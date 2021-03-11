package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.service.AssetService
import one.mixin.android.repository.AssetRepository
import one.mixin.android.vo.Asset

@HiltWorker
class RefreshAssetsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val assetRepo: AssetRepository
) : BaseWork(context, parameters) {
    companion object {
        const val ASSET_ID = "asset_id"
    }

    override suspend fun onRun(): Result {
        val assetId = inputData.getString(ASSET_ID)
        if (assetId != null) {
            val response = assetService.getAssetByIdSuspend(assetId)
            return if (response.isSuccess && response.data != null) {
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
}
