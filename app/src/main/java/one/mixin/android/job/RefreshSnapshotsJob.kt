package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.RxBus
import one.mixin.android.event.RefreshSnapshotEvent
import one.mixin.android.vo.Snapshot

class RefreshSnapshotsJob(
    private val assetId: String? = null,
    private val offset: String? = null,
    private val opponent: String? = null,
) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() = runBlocking {
        val response = if (assetId == null) {
            tokenService.getAllSnapshots(offset, opponent = opponent)
        } else {
            // Todo replace
            tokenService.getSnapshotsByAssetId("43d61dcd-e413-450d-80b8-101d5e903357", offset)
        }
        if (response.isSuccess && response.data != null) {
            val list = response.data as List<Snapshot>
            snapshotDao.insertListSuspend(list)
            list.forEach { item ->
                if (tokenDao.simpleAsset(item.assetId) == null) {
                    jobManager.addJobInBackground(RefreshTokensJob(item.assetId))
                }
            }
            list.lastOrNull()?.let {
                RxBus.publish(RefreshSnapshotEvent(it.createdAt))
            }
        }
    }
}
