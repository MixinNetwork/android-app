package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_SNAPSHOT_OFFSET
import one.mixin.android.RxBus
import one.mixin.android.db.property.PropertyHelper
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
        private const val LIMIT = 30
    }

    override fun onRun() =
        runBlocking {
            val response =
                if (assetId == null) {
                    val startPosition = offset ?: PropertyHelper.findValueByKey(PREF_SNAPSHOT_OFFSET, TIME_ZERO)
                    tokenService.getSnapshots(startPosition, opponent = opponent, limit = LIMIT)
                } else {
                    // todo maybe remove
                    tokenService.getSnapshots(assetId = assetId, offset ?: safeSnapshotDao.getLastItemCreate(assetId) ?: TIME_ZERO, limit = LIMIT)
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
                    if (assetId == null) {
                        PropertyHelper.updateKeyValue(PREF_SNAPSHOT_OFFSET, it.createdAt)
                    }
                    RxBus.publish(RefreshSnapshotEvent(it.createdAt))
                }
            }
        }
}
