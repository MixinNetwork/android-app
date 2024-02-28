package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.RxBus
import one.mixin.android.event.RefreshSnapshotEvent
import one.mixin.android.vo.safe.SafeSnapshot
import org.threeten.bp.Instant

class RefreshSnapshotsJob(
    private val assetId: String? = null,
    private val offset: String? = null,
    private val opponent: String? = null,
) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork()) {
    companion object {
        private const val serialVersionUID = 1L
        private val TIME_ZERO: String = Instant.ofEpochMilli(0).toString()
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() =
        runBlocking {
            val response =
                if (assetId == null) {
                    tokenService.getSnapshots(offset = offset ?: TIME_ZERO, opponent = opponent)
                } else {
                    tokenService.getSnapshots(assetId = assetId, offset ?: safeSnapshotDao.getLastItemCreate(assetId) ?: TIME_ZERO)
                }
            if (response.isSuccess && response.data != null) {
                val list = response.data as List<SafeSnapshot>
                safeSnapshotDao.insertListSuspend(list)
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
