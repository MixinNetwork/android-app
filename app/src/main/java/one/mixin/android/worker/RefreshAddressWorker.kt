package one.mixin.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AddressDao

class RefreshAddressWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val addressDao: AddressDao
) : BaseWork(context, parameters) {

    companion object {
        const val ASSET_ID = "asset_id"
    }

    override suspend fun onRun(): Result {
        val assetId = inputData.getString(ASSET_ID) ?: return Result.failure()
        val response = assetService.addresses(assetId).execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            response.data?.let {
                addressDao.insertList(it)
            }
            Result.success()
        } else {
            Result.failure()
        }
    }
}
