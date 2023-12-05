package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.session.Session
import one.mixin.android.session.buildHashMembers
import timber.log.Timber

class ForceSyncOutputJob(private val offset: Long, private val kernelAssetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).groupBy(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "ForceSyncOutputJob"

        private const val syncOutputLimit = 200
    }

    override fun onRun() =
        runBlocking {
            syncOutputs(offset)
        }

    private tailrec suspend fun syncOutputs(sequence: Long? = null) {
        Timber.d("$TAG sync outputs sequence: $sequence")
        val userId = requireNotNull(Session.getAccountId())
        val members = buildHashMembers(listOf(userId))
        val resp = utxoService.getOutputs(members, 1, sequence, syncOutputLimit, asset = kernelAssetId)
        if (!resp.isSuccess || resp.data.isNullOrEmpty()) {
            Timber.d("$TAG getOutputs ${resp.isSuccess}, ${resp.data.isNullOrEmpty()}")
            return
        }
        val outputs = (requireNotNull(resp.data) { "outputs can not be null or empty at this step" })
        if (outputs.isNotEmpty()) {
            // Insert replace
            outputDao.insertList(outputs)
        }
        Timber.d("$TAG insertOutputs ${outputs.size}")
        if (outputs.size >= syncOutputLimit) {
            syncOutputs(outputDao.findLatestOutputSequence())
        } else {
            jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(kernelAssetId)))
        }
    }
}
