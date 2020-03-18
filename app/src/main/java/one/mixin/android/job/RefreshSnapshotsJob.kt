package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.Snapshot

class RefreshSnapshotsJob(
    private val assetId: String? = null,
    private val offset: String = "",
    private val limit: Int = LIMIT
) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() {
        val response = if (assetId == null) {
            assetService.allSnapshots(offset, limit).execute().body()
        } else {
            assetService.snapshots(assetId, offset, limit).execute().body()
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
