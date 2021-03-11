package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AssetDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.vo.Snapshot

@HiltWorker
class RefreshUserSnapshotsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val assetService: AssetService,
    private val snapshotDao: SnapshotDao,
    private val assetDao: AssetDao
) : BaseWork(context, parameters) {

    companion object {
        const val USER_ID = "user_id"
    }

    override suspend fun onRun(): Result {
        val userId = inputData.getString(USER_ID) ?: return Result.failure()
        val response = assetService.getAllSnapshots(opponent = userId)
        return if (response.isSuccess && response.data != null) {
            val list = response.data as List<Snapshot>
            snapshotDao.insertList(list)
            list.forEach { item ->
                if (assetDao.simpleAsset(item.assetId) == null) {
                    WorkManager.getInstance(MixinApplication.appContext).enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>(
                        workDataOf(RefreshAssetsWorker.ASSET_ID to item.assetId)
                    )
                }
            }
            Result.success()
        } else {
            Result.failure()
        }
    }
}
