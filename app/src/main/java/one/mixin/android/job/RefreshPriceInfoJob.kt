package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class RefreshPriceInfoJob(private val assetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshPriceInfoJob"
    }

    override fun onRun() = runBlocking{
        val response = routeService.price(assetId)
        if (response.isSuccess && response.data != null) {
            response.data?.let {
                priceInfoDao.insert(it)
            }
        }
    }
}
