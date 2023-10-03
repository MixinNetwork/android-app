package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import okio.internal.commonToUtf8String
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.getRFC3339Nano
import one.mixin.android.session.Session
import timber.log.Timber

class SyncOutputJob : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"

        private const val syncOutputLimit = 200
    }

    override fun onRun() = runBlocking {
        syncOutputs()
    }

    private tailrec suspend fun syncOutputs() {
        val latestOutputCreatedAt = outputDao.findLatestOutputCreatedAt()
        Timber.d("$TAG sync outputs latestOutputCreatedAt: $latestOutputCreatedAt")
        val userId = requireNotNull(Session.getAccountId())
        val members = buildHashMembers(listOf(userId))
        val resp = utxoService.getOutputs(members, 1, latestOutputCreatedAt?.getRFC3339Nano(), syncOutputLimit)
        if (!resp.isSuccess || resp.data.isNullOrEmpty()) {
            Timber.d("$TAG getOutputs ${resp.isSuccess}, ${resp.data.isNullOrEmpty()}")
            return
        }
        val outputs = (requireNotNull(resp.data) { "outputs can not be null or empty at this step" })
        outputDao.insertListSuspend(outputs)
        if (outputs.size <= syncOutputLimit) {
            syncOutputs()
        } else {
            jobManager.addJobInBackground(ProcessUtxoJob())
        }
    }

    private fun buildHashMembers(ids: List<String>): String {
        return ids.sortedBy { it }
            .joinToString("")
            .sha3Sum256()
            .commonToUtf8String()
    }
}
