package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.service.AddressService
import one.mixin.android.db.AddressDao

@HiltWorker
class RefreshAddressWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val addressService: AddressService,
    private val addressDao: AddressDao
) : BaseWork(context, parameters) {

    companion object {
        const val ASSET_ID = "asset_id"
    }

    override suspend fun onRun(): Result {
        val assetId = inputData.getString(ASSET_ID) ?: return Result.failure()
        val response = addressService.addresses(assetId).execute().body()
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
