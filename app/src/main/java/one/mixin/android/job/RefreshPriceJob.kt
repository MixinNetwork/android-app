package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class RefreshPriceJob(private val assetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork().persist(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshPriceJob"
    }

    override fun onRun() = runBlocking{
        val response = routeService.priceHistory(assetId, "1D")
        if (response.isSuccess && response.data != null) {
            response.data?.let {
                historyPriceDao.insert(it)
            }
        }
    }
}
