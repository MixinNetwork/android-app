package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.google.gson.annotations.SerializedName
import kernel.Kernel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toHex
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import one.mixin.android.util.uniqueObjectId
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.safe.OutputState
import one.mixin.android.vo.safe.RawTransactionType
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeSnapshotType
import timber.log.Timber
import java.util.UUID

class RestoreTransactionJob : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"
    }

    override fun onRun() =
        runBlocking(
            CoroutineExceptionHandler { _, error ->
                reportException(error)
                Timber.e(error)
            },
        ) {
            while (true) {
                val transaction =
                    rawTransactionDao.findUnspentTransaction() ?: return@runBlocking

                val feeTraceId = uniqueObjectId(transaction.requestId, "FEE")
                val feeTransaction = rawTransactionDao.findRawTransaction(feeTraceId, RawTransactionType.FEE.value)
                try {
                    val response = utxoService.getTransactionsById(transaction.requestId)
                    if (response.isSuccess) {
                        val rawTx = Kernel.decodeRawTx(transaction.rawTransaction, 0)
                        val transactionsData = GsonHelper.customGson.fromJson(rawTx, TransactionsData::class.java)
                        runInTransaction {
                            rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                            rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                        }
                        val token = tokenDao.findTokenByAsset(transactionsData.asset)
                        if (token?.assetId == null) {
                            return@runBlocking
                        }
                        if (feeTransaction == null) {
                            val data = response.data!!
                            if (transaction.receiverId.isNotBlank()) {
                                insertSnapshotMessage(data, transaction.receiverId)
                            }
                        }
                    } else if (response.errorCode == 404) {
                        val rawTx = Kernel.decodeRawTx(transaction.rawTransaction, 0)
                        val transactionsData = GsonHelper.customGson.fromJson(rawTx, TransactionsData::class.java)
                        val token = tokenDao.findTokenByAsset(transactionsData.asset)
                        if (token?.assetId == null) {
                            throw IllegalArgumentException("Lost token ${transactionsData.asset}")
                        }
                        val transactionRsp =
                            utxoService.transactions(
                                if (feeTransaction != null) {
                                    listOf(TransactionRequest(transaction.rawTransaction, transaction.requestId), TransactionRequest(feeTransaction.rawTransaction, feeTransaction.requestId))
                                } else {
                                    listOf(TransactionRequest(transaction.rawTransaction, transaction.requestId))
                                },
                            )
                        if (transactionRsp.error == null) {
                            val transactionResponse = transactionRsp.data!!.first()
                            runInTransaction {
                                rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                                rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                            }
                            if (feeTransaction == null && transaction.receiverId.isNotBlank()) {
                                insertSnapshotMessage(transactionResponse, transaction.receiverId)
                            }
                        } else {
                            reportException(e = Throwable("Transaction Error ${transactionRsp.errorDescription}"))
                            rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                            rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                        }
                        jobManager.addJobInBackground(SyncOutputJob())
                    } else if (response.errorCode >= 500) {
                        reportException(Exception("Restore Transaction Error${transaction.requestId} - ${response.errorCode}"))
                        delay(3000)
                    } else {
                        reportException(Exception("Restore Transaction Error${transaction.requestId} - ${response.errorCode}"))
                        rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                        rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    reportException(e)
                    delay(3000)
                }
            }
        }

    private fun insertSnapshotMessage(
        data: TransactionResponse,
        opponentId: String,
    ) {
        val conversationId = generateConversationId(data.userId, opponentId)
        initConversation(conversationId, data.userId, opponentId)
        val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, MessageCategory.SYSTEM_SAFE_SNAPSHOT.name, "", data.createdAt, MessageStatus.DELIVERED.name, SafeSnapshotType.transfer.name, null, data.snapshotId)
        appDatabase.insertMessage(message)
        MessageFlow.insert(message.conversationId, message.messageId)
    }

    private fun initConversation(
        conversationId: String,
        senderId: String,
        recipientId: String,
    ) {
        val c = conversationDao.findConversationById(conversationId)
        if (c != null) return
        val createdAt = nowInUtc()
        val conversation =
            createConversation(
                conversationId,
                ConversationCategory.CONTACT.name,
                recipientId,
                ConversationStatus.START.ordinal,
            )
        val participants =
            arrayListOf(
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
    val asset: String,
    @SerializedName("Extra")
    val extra: String?,
    @SerializedName("Inputs")
    val inputs: List<Input>,
)

class Input(
    @SerializedName("Hash")
    val hash: String,
    @SerializedName("Index")
    val index: Int,
)
