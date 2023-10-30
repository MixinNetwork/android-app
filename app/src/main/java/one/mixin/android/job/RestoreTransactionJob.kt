package one.mixin.android.job

import TransactionResponse
import com.birbit.android.jobqueue.Params
import com.google.gson.annotations.SerializedName
import kernel.Kernel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.safe.SafeSnapshot
import timber.log.Timber
import java.util.UUID

class RestoreTransactionJob() : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"
    }

    override fun onRun() = runBlocking(CoroutineExceptionHandler { _, error ->
        Timber.e(error)
    }) {
        rawTransactionDao.findTransactions().forEach { transition ->
            try {
                val response = utxoService.getTransactionsById(transition.requestId)
                if (response.isSuccess) {
                    val rawTx = Kernel.decodeRawTx(transition.rawTransaction, 0)
                    val transactionsData = GsonHelper.customGson.fromJson(rawTx, TransactionsData::class.java)
                    val outputIds = transactionsData.inputs.map {
                        UUID.nameUUIDFromBytes("${it.hash}:${it.index}".toByteArray()).toString()
                    }
                    outputDao.updateUtxoToSigned(outputIds)
                    rawTransactionDao.deleteById(transition.requestId)
                } else if (response.errorCode == 404) {
                    val rawTx = Kernel.decodeRawTx(transition.rawTransaction, 0)
                    val transactionsData = GsonHelper.customGson.fromJson(rawTx, TransactionsData::class.java)
                    val token = tokenDao.findTokenByAsset(transactionsData.asset)
                    if (token?.assetId == null) {
                        rawTransactionDao.deleteById(transition.requestId)
                    }
                    val outputIds = transactionsData.inputs.map {
                        UUID.nameUUIDFromBytes("${it.hash}:${it.index}".toByteArray()).toString()
                    }
                    val transactionRsp = utxoService.transactions(TransactionRequest(transition.rawTransaction, transition.requestId))
                    if (transactionRsp.error == null) {
                        outputDao.updateUtxoToSigned(outputIds)
                        rawTransactionDao.deleteById(transactionRsp.data!!.requestId)
                        insertSnapshotMessage(transactionRsp.data!!, token!!.assetId, transactionRsp.data!!.amount, transition.receiverId, transactionsData.extra?.decodeBase64()?.decodeToString())
                    } else {
                        rawTransactionDao.deleteById(transactionRsp.data!!.requestId)
                    }
                    jobManager.addJobInBackground(SyncOutputJob())
                } else {
                    rawTransactionDao.deleteById(transition.requestId)
                }
            } catch (e: Exception) {
                rawTransactionDao.deleteById(transition.requestId)
            }
        }
    }

    private fun insertSnapshotMessage(data: TransactionResponse, assetId: String, amount: String, opponentId: String, memo: String?) {
        val snapshotId =  UUID.nameUUIDFromBytes("${data.userId}:${data.transactionHash}".toByteArray()).toString()
        val conversationId = generateConversationId(data.userId, opponentId)
        initConversation(conversationId, data.userId, opponentId)
        val snapshot = SafeSnapshot(snapshotId, SnapshotType.transfer.name, assetId, "-${amount}", data.createdAt, data.userId, null, "", null, null, memo ?: "", null, "", null, null)
        val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, MessageCategory.SYSTEM_SAFE_SNAPSHOT.name, "", data.createdAt, MessageStatus.DELIVERED.name, snapshot.type, null, snapshot.snapshotId)
        safeSnapshotDao.insert(snapshot)
        appDatabase.insertMessage(message)
        MessageFlow.insert(message.conversationId, message.messageId)
    }

    private fun initConversation(conversationId: String, senderId: String, recipientId: String) {
        val c = conversationDao.findConversationById(conversationId)
        if (c != null) return
        val createdAt = nowInUtc()
        val conversation = createConversation(
            conversationId,
            ConversationCategory.CONTACT.name,
            recipientId,
            ConversationStatus.START.ordinal,
        )
        val participants = arrayListOf(
            Participant(conversationId, senderId, "", createdAt),
            Participant(conversationId, recipientId, "", createdAt),
        )
        appDatabase.runInTransaction {
            conversationDao.upsert(conversation)
            participantDao.insertList(participants)
        }
        jobManager.addJobInBackground(RefreshConversationJob(conversationId))
    }
}

class TransactionsData(
    @SerializedName("Asset")
    val asset:String,
    @SerializedName("Extra")
    val extra:String?,
    @SerializedName("Inputs")
    val inputs:List<Input>
)

class Input(
    @SerializedName("Hash")
    val hash: String,
    @SerializedName("Index")
    val index: Int
)