package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_COLLECTION
import one.mixin.android.db.property.PropertyHelper
import timber.log.Timber

class InscriptionCollectionMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "inscription_collection_migration"
    }

    override fun onRun() =
        runBlocking {
            val allHash = inscriptionCollectionDao.allCollectionHash()
            allHash.forEach {
                if (!syncInscriptionCollection(it)) {
                    // Wait next timing
                    return@runBlocking
                }
            }
            PropertyHelper.updateKeyValue(PREF_MIGRATION_COLLECTION, true)
        }

    private suspend fun syncInscriptionCollection(collectionHash: String): Boolean {
        val collectionResponse = tokenService.getInscriptionCollection(collectionHash)
        if (collectionResponse.isSuccess) {
            val inscriptionCollection = collectionResponse.data ?: return false
            inscriptionCollectionDao.insert(inscriptionCollection)
            return true
        } else {
            Timber.e(collectionResponse.errorDescription)
            return false
        }
    }
}
