package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_SNAPSHOT_OFFSET
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.safe.SafeSnapshot
import org.threeten.bp.Instant

class RefreshSnapshotsJob : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork()) {
    companion object {
        private const val serialVersionUID = 2L
        private val TIME_ZERO: String = Instant.ofEpochMilli(0).toString()
        const val GROUP = "RefreshSnapshotsJob"
    }

    override fun onRun() =
        runBlocking {
            val startPosition = PropertyHelper.findValueByKey(PREF_SNAPSHOT_OFFSET, TIME_ZERO)
            val response = tokenService.getSnapshots(offset = startPosition, limit = LIMIT)
            if (response.isSuccess && response.data != null) {
                val list = response.data as List<SafeSnapshot>
                safeSnapshotDao.insertListSuspend(list)
                list.forEach { item ->
                    if (tokenDao.simpleAsset(item.assetId) == null) {
                        jobManager.addJobInBackground(RefreshTokensJob(item.assetId))
                    }
                }
                list.lastOrNull()?.let {
                    PropertyHelper.updateKeyValue(PREF_SNAPSHOT_OFFSET, it.createdAt)
                    if (list.size >= LIMIT) {
                        jobManager.addJobInBackground(RefreshSnapshotsJob())
                    }
                }
            }
        }
}
