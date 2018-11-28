package one.mixin.android.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AssetService
import one.mixin.android.di.worker.AndroidWorkerInjector
import one.mixin.android.repository.AssetRepository
import one.mixin.android.vo.Asset
import javax.inject.Inject

class RefreshAssetsWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    @Inject
    lateinit var assetService: AssetService
    @Inject
    lateinit var assetRepo: AssetRepository

    companion object {
        const val ASSET_ID = "asset_id"
    }

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        val assetId = inputData.getString(ASSET_ID)
        return try {
            if (assetId != null) {
                val response = assetService.asset(assetId).execute().body()
                return if (response != null && response.isSuccess && response.data != null) {
                    response.data.let {
                        assetRepo.upsert(it!!)
                    }
                    Result.SUCCESS
                } else {
                    Result.FAILURE
                }
            } else {
                val response = assetService.assets().execute().body()
                return if (response != null && response.isSuccess && response.data != null) {
                    val list = response.data as List<Asset>
                    for (item in list) {
                        assetRepo.upsert(item)
                    }
                    Result.SUCCESS
                } else {
                    Result.FAILURE
                }
            }
        } catch (e: Exception) {
            Result.FAILURE
        }
    }
}