package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class RefreshWeb3TokenJob(
    private val walletId: String,
    private val assetId: String
) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().setGroupId(GROUP)) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3TokenJob"
    }

    override fun onRun(): Unit =
        runBlocking {
            try {
                // Todo refresh
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
}
