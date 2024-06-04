package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.reportException
import one.mixin.android.util.uniqueObjectId
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.OutputState
import one.mixin.android.vo.safe.RawTransactionType
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
                    Timber.e("Restore Transaction(${transaction.requestId}): Get Transaction")
                    val response = utxoService.getTransactionsById(transaction.requestId)
                    if (response.isSuccess) {
                        Timber.e("Restore Transaction(${transaction.requestId}): db begin")
                        runInTransaction {
                            Timber.e("Restore Transaction(${transaction.requestId}): update raw transaction ${transaction.requestId}")
                            rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                            Timber.e("Restore Transaction(${transaction.requestId}): update raw transaction $feeTraceId")
                            rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                        }
                        Timber.e("Restore Transaction(${transaction.requestId}): db end")
                        if (feeTransaction == null) {
                            val data = response.data!!
                            if (transaction.receiverId.isNotBlank()) {
                                insertSnapshotMessage(data, transaction.receiverId, transaction.inscriptionHash)
                            }
                        }
                    } else if (response.errorCode == 404) {
                        Timber.e("Restore Transaction(${transaction.requestId}): Post Transaction")
                        val transactionRsp =
                            utxoService.transactions(
                                if (feeTransaction != null) {
                                    listOf(TransactionRequest(transaction.rawTransaction, transaction.requestId), TransactionRequest(feeTransaction.rawTransaction, feeTransaction.requestId))
                                } else {
                                    listOf(TransactionRequest(transaction.rawTransaction, transaction.requestId))
                                },
                            )
                        if (transactionRsp.error == null) {
                            Timber.e("Restore Transaction(${transaction.requestId}): Post Transaction Success")
                            val transactionResponse = transactionRsp.data!!.first()
                            Timber.e("Restore Transaction(${transaction.requestId}): db begin")
                            runInTransaction {
                                Timber.e("Restore Transaction(${transaction.requestId}): update raw transaction ${transaction.requestId}")
                                rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                                Timber.e("Restore Transaction(${transaction.requestId}): update raw transaction $feeTraceId")
                                rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                            }
                            Timber.e("Restore Transaction(${transaction.requestId}): db end")
                            if (feeTransaction == null && transaction.receiverId.isNotBlank()) {
                                insertSnapshotMessage(transactionResponse, transaction.receiverId, transaction.inscriptionHash)
                            }
                        } else {
                            Timber.e("Restore Transaction(${transaction.requestId}): Post Transaction Error ${transactionRsp.errorDescription}")
                            reportException(e = Throwable("Transaction Error ${transactionRsp.errorDescription}"))
                            rawTransactionDao.updateRawTransaction(transaction.requestId, OutputState.signed.name)
                            rawTransactionDao.updateRawTransaction(feeTraceId, OutputState.signed.name)
                        }
                        jobManager.addJobInBackground(SyncOutputJob())
                    } else if (response.errorCode >= 500) {
                        Timber.e("Restore Transaction(${transaction.requestId}): GET Transaction Server Error")
                        reportException(Exception("Restore Transaction Error: ${transaction.requestId} - ${response.errorCode}"))
                        delay(3000)
                    } else {
                        Timber.e("Restore Transaction(${transaction.requestId}): GET Transaction Error ${response.errorDescription}")
                        reportException(Exception("Restore Transaction Error: ${transaction.requestId} - ${response.errorCode}"))
                        // do nothing, waiting cycle
                        delay(3000)
                    }
                } catch (e: Exception) {
                    Timber.e("Restore Transaction(${transaction.requestId}): Throwable ${e.message}")
                    Timber.e(e)
                    reportException(e)
                    delay(3000)
                }
            }
        }

    private fun insertSnapshotMessage(
        data: TransactionResponse,
        opponentId: String,
        inscriptionHash: String?,
    ) {
        val user = userDao.findUser(opponentId)
        if (user != null && !user.notMessengerUser()) {
            val conversationId = generateConversationId(data.userId, opponentId)
            initConversation(conversationId, data.userId, opponentId)
            val category =
                if (inscriptionHash != null) {
                    MessageCategory.SYSTEM_SAFE_INSCRIPTION.name
                } else {
                    MessageCategory.SYSTEM_SAFE_SNAPSHOT.name
                }
            val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, category, inscriptionHash ?: "", data.createdAt, MessageStatus.DELIVERED.name, SafeSnapshotType.snapshot.name, null, data.getSnapshotId)
            appDatabase.insertMessage(message)
            if (inscriptionHash != null) {
                jobManager.addJobInBackground(SyncInscriptionMessageJob(conversationId, message.messageId, inscriptionHash, data.getSnapshotId))
            }
            MessageFlow.insert(message.conversationId, message.messageId)
        }
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
