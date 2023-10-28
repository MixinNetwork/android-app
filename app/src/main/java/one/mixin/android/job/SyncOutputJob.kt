package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.session.Session
import one.mixin.android.session.buildHashMembers
import timber.log.Timber

class SyncOutputJob() : BaseJob(
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
        val local = outputDao.findOutputs(outputs.map { it.outputId })
        val result = outputs.filter { online ->
            local.none { localData -> localData == online || (localData.outputId == online.outputId && localData.state == "signed") }
        }
        if (result.isNotEmpty()) {
            outputDao.insertListSuspend(result)
        }
        Timber.d("$TAG insertOutputs ${result.size}")
        if (outputs.size >= syncOutputLimit) {
            syncOutputs()
        }
    }

}
