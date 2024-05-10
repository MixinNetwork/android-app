package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class SyncInscriptionsJob(val hash: List<String>) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).groupBy(TAG).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncInscriptionJob"
    }

    override fun onRun() =
        runBlocking {
            syncInscriptions()
        }

    private suspend fun syncInscriptions() {
        val local = inscriptionDao.getExitsHash(hash)
        (hash - local.toSet()).forEach {
            val response = tokenService.getInscriptionItem(it)
            if (response.isSuccess) {
                val inscription = response.data ?: return@forEach
                inscriptionDao.insert(inscription)
                syncInscriptionCollection(inscription.collectionHash)
            } else {
                Timber.e(response.errorDescription)
            }
        }
        inscriptionDao.getInscriptionCollectionIds(hash).forEach {
            syncInscriptionCollection(it)
        }
    }

    private suspend fun syncInscriptionCollection(collectionHash: String) {
        if (inscriptionCollectionDao.exits(collectionHash) == null) {
            val collectionResponse = tokenService.getInscriptionCollection(collectionHash)
            if (collectionResponse.isSuccess) {
                val inscriptionCollection = collectionResponse.data ?: return
                inscriptionCollectionDao.insert(inscriptionCollection)
            } else {
                Timber.e(collectionResponse.errorDescription)
            }
        }
    }
}
