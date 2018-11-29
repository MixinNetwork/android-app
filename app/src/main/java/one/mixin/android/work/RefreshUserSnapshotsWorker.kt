package one.mixin.android.work

import android.content.Context
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AssetDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.di.worker.AndroidWorkerInjector
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.vo.Snapshot
import javax.inject.Inject

class RefreshUserSnapshotsWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    companion object {
        const val USER_ID = "user_id"
    }

    @Inject
    lateinit var assetService: AssetService

    @Inject
    lateinit var snapshotDao: SnapshotDao

    @Inject
    lateinit var assetDao: AssetDao

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        val userId = inputData.getString(USER_ID) ?: return Result.FAILURE
        return try {
            val response = assetService.mutualSnapshots(userId).execute().body()
            return if (response != null && response.isSuccess && response.data != null) {
                val list = response.data as List<Snapshot>
                snapshotDao.insertList(list)
                list.forEach { item ->
                    if (assetDao.simpleAsset(item.assetId) == null) {
                        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>(
                            workDataOf(RefreshAssetsWorker.ASSET_ID to item.assetId)
                        )
                    }
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