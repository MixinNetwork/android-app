package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.tip.wc.WCChangeEvent
import one.mixin.android.tip.wc.internal.supportChainList
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ChainDapp

class RefreshDappJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshDappJob"
    }

    override fun onRun(): Unit =
        runBlocking {
            userRepo.getBotPublicKey(ROUTE_BOT_USER_ID, false)
            val response = routeService.dapps()
            if (response.isSuccess && response.data != null) {
                saveChainDapps(response.data!!)
                RxBus.publish(WCChangeEvent())
            } else if (response.errorCode == 401) {
                userRepo.getBotPublicKey(ROUTE_BOT_USER_ID, true)
            } else {
                delay(3000)
                jobManager.addJobInBackground(RefreshDappJob())
            }
        }

    internal fun saveChainDapps(chainDapps: List<ChainDapp>) {
        val preferences = MixinApplication.appContext.defaultSharedPreferences
        chainDapps.forEach { chainDapp ->
            val chain = supportChainList.firstOrNull { it.assetId == chainDapp.chainId } ?: return@forEach
            preferences.putString("dapp_${chain.chainId}", GsonHelper.customGson.toJson(chainDapp.dapps))
            chainDapp.rpcUrls.firstNotNullOfOrNull { it.toHttpUrlOrNull() }?.let { rpcUrl ->
                preferences.putString(chain.chainId, rpcUrl.toString())
            }
        }
    }
}
