package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class SyncInscriptionCollectionJob(val hash: String) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).groupBy(TAG).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncInscriptionJob"
    }

    override fun onRun() =
        runBlocking {
            syncInscriptionCollection(hash)
        }

    private suspend fun syncInscriptionCollection(collectionHash: String) {
        val collectionResponse = tokenService.getInscriptionCollection(collectionHash)
        if (collectionResponse.isSuccess) {
            val inscriptionCollection = collectionResponse.data ?: return
            inscriptionCollectionDao.insert(inscriptionCollection)
        } else {
            Timber.e(collectionResponse.errorDescription)
        }
    }
}
