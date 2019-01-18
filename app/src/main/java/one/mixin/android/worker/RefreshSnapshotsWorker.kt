package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AssetDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.vo.Snapshot

class RefreshSnapshotsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val assetDao: AssetDao,
    private val snapshotDao: SnapshotDao
) : BaseWork(context, parameters) {

    companion object {
        const val ASSET_ID = "asset_id"
    }

    override fun onRun(): Result {
        val assetId = inputData.getString(ASSET_ID)
        val response = if (assetId == null) {
            assetService.allSnapshots().execute().body()
        } else {
            assetService.snapshots(assetId).execute().body()
        }
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
            Result.success()
        } else {
            Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}