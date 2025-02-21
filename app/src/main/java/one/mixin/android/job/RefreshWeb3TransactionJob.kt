package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class RefreshWeb3TransactionJob(
    val address: String,
    val chainId: String,
    val fungibleId: String,
    val assetKey: String
) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3Job"
    }

    override fun onRun(): Unit =
        runBlocking {
            var response = web3Service.transactions(address, chainId, fungibleId = fungibleId, assetKey = assetKey)
            if (response.isSuccess) {
                web3TransactionDao.insertListSuspend(response.data!!)
            }
        }
}
