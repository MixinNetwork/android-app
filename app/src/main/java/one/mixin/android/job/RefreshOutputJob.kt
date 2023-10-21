package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class RefreshOutputJob(val hash:ArrayList<String>) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"
    }

    override fun onRun() = runBlocking {
        outputDao.signedUtxo(hash)
        // todo sync utxo state
        // val ids = outputDao.findUtxoIds(hash)
        // if (ids.isNotEmpty()){
            // val response = utxoService.fetch(OutputFetchRequest(Session.getAccountId()!!, ids))
            // if (response.isSuccess && !response.data.isNullOrEmpty()){
            //     val outputs = (requireNotNull(response.data) { "outputs can not be null or empty at this step" })
            //     outputDao.insertListSuspend(outputs)
            // }
        // }
    }

}
