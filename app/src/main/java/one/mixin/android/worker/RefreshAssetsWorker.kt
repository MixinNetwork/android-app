package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AssetService
import one.mixin.android.repository.AssetRepository
import one.mixin.android.vo.Asset
import javax.inject.Inject

class RefreshAssetsWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {

    @Inject
    lateinit var assetService: AssetService
    @Inject
    lateinit var assetRepo: AssetRepository

    companion object {
        const val ASSET_ID = "asset_id"
    }

    override fun onRun(): Result {
        val assetId = inputData.getString(ASSET_ID)
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
    }
}