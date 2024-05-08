package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.SafeInscription
import timber.log.Timber

class SyncInscriptionMessageJob(val conversationId:String, val messageId: String, val hash: String?, val snapshotId: String?) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).groupBy(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncInscriptionMessageJob"
    }

    override fun onRun() =
        runBlocking {
            syncInscription()
        }

    private suspend fun syncInscription() {
        val inscriptionHash = hash ?: snapshotId?.let { safeSnapshotDao.findHashBySnapshotId(snapshotId) } ?: return
        var inscription = inscriptionDao.findInscriptionByHash(inscriptionHash)
        if (inscription == null) {
            val response = tokenService.getInscriptionItem(inscriptionHash)
            if (response.isSuccess) {
                inscription = response.data ?: return
                inscriptionDao.insert(inscription)
            } else {
                Timber.e(response.errorDescription)
            }
        }
        inscription ?: return
        var inscriptionCollection = inscriptionCollectionDao.findInscriptionCollectionByHash(inscription.collectionHash)
        if (inscriptionCollection == null) {
            val collectionResponse = tokenService.getInscriptionCollection(inscription.collectionHash)
            if (collectionResponse.isSuccess) {
                inscriptionCollection = collectionResponse.data ?: return
                inscriptionCollectionDao.insert(inscriptionCollection)
            } else {
                Timber.e(collectionResponse.errorDescription)
            }
        }
        inscriptionCollection ?: return
        messageDao.updateMessageContent(
            GsonHelper.customGson.toJson(
                SafeInscription(
                    inscriptionCollection.collectionHash,
                    inscription.inscriptionHash,
                    inscription.sequence,
                    inscriptionCollection.name,
                    inscription.contentType,
                    inscription.contentURL,
                    inscriptionCollection.iconURL
                )
            ), messageId
        )
        MessageFlow.update(conversationId, messageId)
    }
}
