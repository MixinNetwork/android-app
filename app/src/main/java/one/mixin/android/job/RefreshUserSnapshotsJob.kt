package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.Snapshot

class RefreshUserSnapshotsJob(private val userId: String)
    : BaseJob(Params(PRIORITY_BACKGROUND).addTags(RefreshSnapshotsJob.GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() {
        val response = assetService.mutualSnapshots(userId).execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val list = response.data as List<Snapshot>
            snapshotDao.insertList(list)
        }
    }
}