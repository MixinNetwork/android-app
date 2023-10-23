package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kernel.Kernel
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class RestoreTransactionJob() : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SyncOutputJob"
    }

    override fun onRun() = runBlocking {
        rawTransactionDao.findTransactions().forEach {transition->
            val s = Kernel.decodeRawTx(transition.rawTransaction, 0)
            // Todo check transaction state by transaction hash
            Timber.e(s)
        }
    }
}
