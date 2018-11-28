package one.mixin.android.job

import androidx.work.WorkManager
import androidx.work.workDataOf
import com.birbit.android.jobqueue.Params
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.vo.Snapshot
import one.mixin.android.work.RefreshAssetsWorker

class RefreshSnapshotsJob(private val assetId: String? = null)
    : BaseJob(Params(PRIORITY_BACKGROUND).addTags(RefreshSnapshotsJob.GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() {
        val response = if (assetId == null) {
            assetService.allSnapshots().execute().body()
        } else {
            assetService.snapshots(assetId).execute().body()
        }
        if (response != null && response.isSuccess && response.data != null) {
            val list = response.data as List<Snapshot>
            snapshotDao.insertList(list)
            list.forEach { item ->
                if (assetDao.simpleAsset(item.assetId) == null) {
                    WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>(
                        workDataOf(RefreshAssetsWorker.ASSET_ID to item.assetId)
                    )
                }
            }
        }
    }
}