package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_INSCRIPTION
import one.mixin.android.db.insertUnspentOutputs
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.job.SyncOutputJob.Companion.TAG
import one.mixin.android.session.Session
import one.mixin.android.session.buildHashMembers
import timber.log.Timber

class InscriptionMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "inscription_migration"
        private const val syncOutputLimit = 200
    }

    override fun onRun() =
        runBlocking {
            syncOutputs(0)
            val hash = outputDao.findUnspentInscriptionHash()
            if (hash.isNotEmpty()) {
                jobManager.addJobInBackground(SyncInscriptionsJob(hash))
            }
            PropertyHelper.updateKeyValue(PREF_MIGRATION_INSCRIPTION, true)
        }

    private tailrec suspend fun syncOutputs(sequence: Long) {
        val userId = requireNotNull(Session.getAccountId())
        val members = buildHashMembers(listOf(userId))
        val resp = utxoService.getOutputs(members, 1, sequence, syncOutputLimit, state = "unspent")
        if (!resp.isSuccess || resp.data.isNullOrEmpty()) {
            Timber.d("$TAG getOutputs ${resp.isSuccess}, ${resp.data.isNullOrEmpty()}")
            return
        }
        val outputs = (requireNotNull(resp.data) { "outputs can not be null or empty at this step" })
        if (outputs.isNotEmpty()) {
            outputDao.insertUnspentOutputs(outputs)
            outputs.mapNotNull {it.inscriptionHash }.apply {
                if (isNotEmpty()) {
                    jobManager.addJobInBackground(SyncInscriptionsJob(this))
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
            syncOutputs(outputs.last().sequence)
        }
    }
}
