package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class SyncInscriptionJob(val hash: List<String>) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).groupBy(TAG).requireNetwork(),
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
                if (inscriptionCollectionDao.exits(inscription.collectionHash) == null) {
                    val collectionResponse = tokenService.getInscriptionCollection(inscription.collectionHash)
                    val inscriptionCollection = collectionResponse.data ?: return@forEach
                    inscriptionCollectionDao.insert(inscriptionCollection)
                } else {
                    // Todo
                }
            } else {
                // Todo
            }
        }
    }
}
