package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.Snapshot

class RefreshSnapshotsJob(private val snapshotId: String? = null)
    : BaseJob(Params(PRIORITY_BACKGROUND).addTags(RefreshSnapshotsJob.GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() {
        val response = if (snapshotId == null) {
            assetService.allSnapshots().execute().body()
        } else {
            assetService.snapshots(snapshotId).execute().body()
        }
        if (response != null && response.isSuccess && response.data != null) {
            val list = response.data as List<Snapshot>
            snapshotDao.insertList(list)
            list.forEach { item ->
                if (assetDao.simpleAsset(item.assetId) == null) {
                    jobManager.addJobInBackground(RefreshAssetsJob(item.assetId))
                }
            }
        }
    }
}