package one.mixin.android.job

import TransactionResponse
import com.birbit.android.jobqueue.Params
import com.google.gson.annotations.SerializedName
import kernel.Kernel
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.safe.SafeSnapshot
import java.util.UUID

class RestoreTransactionJob() : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"
    }

    override fun onRun() = runBlocking {
        rawTransactionDao.findTransactions().forEach { transition ->
            val response = utxoService.getTransactionsById(transition.requestId)
            if (response.isSuccess) {
                rawTransactionDao.deleteById(transition.requestId)
            } else if (response.errorCode == 404) {
                val rawTx = Kernel.decodeRawTx(transition.rawTransaction, 0)
                val transactionsData = GsonHelper.customGson.fromJson(rawTx, TransactionsData::class.java)
                val token = tokenDao.findTokenByAsset(transactionsData.asset)
                if (token?.assetId == null) {
                    rawTransactionDao.deleteById(transition.requestId)
                }
                val hash = transactionsData.inputs.map {
                    it.hash
                }
                val transactionRsp = utxoService.transactions(TransactionRequest(transition.rawTransaction, transition.requestId))
                if (transactionRsp.error != null) {
                    // Todo receiverId, memo
                    insertSnapshotMessage(transactionRsp.data!!, token!!.assetId, transactionRsp.data!!.amount, "", null)
                    outputDao.signedUtxo(hash)
                    rawTransactionDao.deleteById(transactionRsp.data!!.requestId)
                } else {
                    rawTransactionDao.deleteById(transactionRsp.data!!.requestId)
                }
                jobManager.addJobInBackground(SyncOutputJob())
            } else{
                rawTransactionDao.deleteById(transition.requestId)
            }
        }
    }

    private fun insertSnapshotMessage(data: TransactionResponse, assetId: String, amount: String, opponentId: String, memo: String?) {
        val snapshotId =  UUID.nameUUIDFromBytes("${data.userId}:${data.transactionHash}".toByteArray()).toString()
        val conversationId = generateConversationId(data.userId, opponentId)
        val snapshot = SafeSnapshot(snapshotId, "snapshot", assetId, "-${amount}", data.snapshotAt, data.userId, null, null, null, null, memo, null, null, null, null)
        val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, MessageCategory.SYSTEM_SAFE_SNAPSHOT.name, "", data.createdAt, MessageStatus.DELIVERED.name, snapshot.type, null, snapshot.snapshotId)
        safeSnapshotDao.insert(snapshot)
        appDatabase.insertMessage(message)
        MessageFlow.insert(message.conversationId, message.messageId)
    }
}

class TransactionsData(
    @SerializedName("Asset")
    val asset:String,
    @SerializedName("Inputs")
    val inputs:List<Input>
)

class Input(
    @SerializedName("Hash")
    val hash: String,
    @SerializedName("Index")
    val index: Int
)