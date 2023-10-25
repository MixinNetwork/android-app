package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.google.gson.annotations.SerializedName
import kernel.Kernel
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.db.runInTransaction
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.utxo.RawTransaction
import one.mixin.android.vo.utxo.changeToOutput
import timber.log.Timber

class RestoreTransactionJob() : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"
    }

    override fun onRun() = runBlocking {
        rawTransactionDao.findTransactions().forEach { transition ->
            val s = Kernel.decodeRawTx(transition.rawTransaction, 0)
            val response = utxoService.getTransactionsById(transition.requestId)
            if (response.isSuccess) {
                rawTransactionDao.deleteById(transition.requestId)
            } else if (response.errorCode == 404) {
                val s = Kernel.decodeRawTx(transition.rawTransaction, 0)
                val transactionsData = GsonHelper.customGson.fromJson(s, TransactionsData::class.java)
                val token = tokenDao.findTokenByAsset(transactionsData.asset)
                val hash = transactionsData.inputs.map {
                    it.hash
                }
                val transactionRsp = utxoService.transactions(TransactionRequest(transition.rawTransaction, transition.requestId))
                if (transactionRsp.error != null) {
                    rawTransactionDao.deleteById(transactionRsp.data!!.requestId)
                } else {
                    rawTransactionDao.deleteById(transactionRsp.data!!.requestId)
                }
                // Todo save message
                // tokenRepository.insertSnapshotMessage(transactionResponse.data!!, assetId, amount, receiverId, memo)
                outputDao.signedUtxo(hash)
                jobManager.addJobInBackground(SyncOutputJob())
            } else{
                rawTransactionDao.deleteById(transition.requestId)
            }
        }
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