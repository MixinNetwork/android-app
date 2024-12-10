package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_SNAPSHOT_OFFSET
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.vo.safe.SafeSnapshot
import org.threeten.bp.Instant

class RefreshSnapshotsJob : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        private val TIME_ZERO: String = Instant.ofEpochMilli(0).toString()
        const val GROUP = "RefreshSnapshotsJob"
        const val LIMIT = 300
    }

    override fun onRun(): Unit =
        runBlocking {
            val startPosition = PropertyHelper.findValueByKey(PREF_SNAPSHOT_OFFSET, TIME_ZERO)
            refreshSnapshots(startPosition)
        }

    private suspend fun refreshSnapshots(offset: String): List<SafeSnapshot>? {
        val response = tokenService.getSnapshots(offset = offset, limit = LIMIT)
        if (response.isSuccess && response.data != null) {
            val snapshots = response.data as List<SafeSnapshot>
            safeSnapshotDao().insertListSuspend(snapshots.map {
                if (it.opponentId.isEmpty()) {
                    mapSnapshot(it)
                } else {
                    it
                }
            })
            snapshots.forEach { item ->
                if (tokenDao().simpleAsset(item.assetId) == null) {
                    jobManager.addJobInBackground(RefreshTokensJob(item.assetId))
                }
            }
            snapshots.lastOrNull()?.let {
                PropertyHelper.updateKeyValue(PREF_SNAPSHOT_OFFSET, it.createdAt)
                if (snapshots.size >= LIMIT) {
                    return refreshSnapshots(it.createdAt)
                }
            }
            snapshots.map { it.opponentId }.distinct().let {
                jobManager.addJobInBackground(RefreshUserJob(it))
            }
            return snapshots
        }
        return null
    }

    private suspend fun mapSnapshot(snapshot: SafeSnapshot): SafeSnapshot {
        val existingSnapshot = safeSnapshotDao().getSnapshotById(snapshot.snapshotId)
        if (existingSnapshot != null && existingSnapshot.opponentId.isNotEmpty()) {
            val updatedSnapshot = snapshot.copy(opponentId = existingSnapshot.opponentId)
            return updatedSnapshot
        } else {
            return snapshot
        }
    }
}
