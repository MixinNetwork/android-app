package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.insertUnspentOutputs
import one.mixin.android.session.Session
import one.mixin.android.session.buildHashMembers
import timber.log.Timber

class SyncOutputJob() : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).groupBy(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"

        private const val syncOutputLimit = 200
    }

    override fun onRun() =
        runBlocking {
            syncOutputs()
        }

    private tailrec suspend fun syncOutputs() {
        val latestOutputSequence = outputDao.findLatestOutputSequence()
        Timber.d("$TAG sync outputs latestOutputCreatedAt: $latestOutputSequence")
        val userId = requireNotNull(Session.getAccountId())
        val members = buildHashMembers(listOf(userId))
        val resp = utxoService.getOutputs(members, 1, latestOutputSequence, syncOutputLimit, state = "unspent")
        if (!resp.isSuccess || resp.data.isNullOrEmpty()) {
            Timber.d("$TAG getOutputs ${resp.isSuccess}, ${resp.data.isNullOrEmpty()}")
            return
        }
        val outputs = (requireNotNull(resp.data) { "outputs can not be null or empty at this step" })
        if (outputs.isNotEmpty()) {
            outputDao.insertUnspentOutputs(outputs)
            outputs.mapNotNull {it.inscriptionHash }.apply {
                if (isNotEmpty()) {
                    jobManager.addJobInBackground(SyncInscriptionJob(this))
                }
            }
            val list =
                arrayListOf<String>().apply {
                    addAll(outputs.groupBy { it.asset }.keys)
                }
            jobManager.addJobInBackground(CheckBalanceJob(list))
        }
        Timber.d("$TAG insertOutputs ${outputs.size}")
        if (outputs.size >= syncOutputLimit) {
            syncOutputs()
        }
    }
}
