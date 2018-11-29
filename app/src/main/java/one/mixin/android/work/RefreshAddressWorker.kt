package one.mixin.android.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AddressDao
import one.mixin.android.di.worker.AndroidWorkerInjector
import javax.inject.Inject

class RefreshAddressWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    companion object {
        const val ASSET_ID = "asset_id"
    }

    @Inject
    lateinit var assetService: AssetService

    @Inject
    lateinit var addressDao: AddressDao

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        val assetId = inputData.getString(ASSET_ID) ?: return Result.FAILURE
        return try {
            val response = assetService.addresses(assetId).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                response.data?.let {
                    addressDao.insertList(it)
                }
                Result.SUCCESS
            } else {
                Result.FAILURE
            }
        } catch (e: Exception) {
            return Result.FAILURE
        }
    }
}