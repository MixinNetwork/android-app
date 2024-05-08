package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_INSCRIPTION
import one.mixin.android.db.property.PropertyHelper

class InscriptionMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "inscription_migration"
    }

    override fun onRun() =
        runBlocking {
            val hash = outputDao.findUnspentInscriptionHash()
            if (hash.isNotEmpty()) {
                jobManager.addJobInBackground(SyncInscriptionsJob(hash))
            }
            PropertyHelper.updateKeyValue(PREF_MIGRATION_INSCRIPTION, true)
        }
}
