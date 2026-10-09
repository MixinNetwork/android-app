package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.RouteBotPublicKey
import one.mixin.android.util.ErrorHandler
import timber.log.Timber

class RefreshPriceJob(private val assetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork().persist(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshPriceJob"
    }

    override fun onRun(): Unit = runBlocking {
        try {
            val response = routeService.priceHistory(assetId, "1D")
            if (response.isSuccess && response.data != null) {
                response.data?.let {
                    if (it.data.isEmpty()) return@let
                    historyPriceDao.insert(it)
                }
            } else if (response.errorCode == ErrorHandler.AUTHENTICATION) {
                RouteBotPublicKey.shared.get(userService::fetchSessionsSuspend, force = true)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
