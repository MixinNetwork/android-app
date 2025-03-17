package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.WEB3_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.tip.wc.WCChangeEvent
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.util.GsonHelper

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
            userRepo.getBotPublicKey(WEB3_BOT_USER_ID, false)
            val response = routeService.dapps()
            if (response.isSuccess && response.data != null) {
                val gson = GsonHelper.customGson
                val chainDapp = response.data!!
                chainDapp.forEach {
                    val rpc = it.rpcUrls.firstOrNull() ?: it.rpc
                    when (it.chainId) {
                        Chain.Ethereum.assetId -> {
                            MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.Ethereum.chainId}", gson.toJson(it.dapps))
                            MixinApplication.appContext.defaultSharedPreferences.putString(Chain.Ethereum.chainId, rpc)
                        }

                        Chain.BinanceSmartChain.assetId -> {
                            MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.BinanceSmartChain.chainId}", gson.toJson(it.dapps))
                            MixinApplication.appContext.defaultSharedPreferences.putString(Chain.BinanceSmartChain.chainId, rpc)
                        }

                        Chain.Polygon.assetId -> {
                            MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.Polygon.chainId}", gson.toJson(it.dapps))
                            MixinApplication.appContext.defaultSharedPreferences.putString(Chain.Polygon.chainId, rpc)
                        }

                        Chain.Solana.assetId -> {
                            MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.Solana.chainId}", gson.toJson(it.dapps))
                            MixinApplication.appContext.defaultSharedPreferences.putString(Chain.Solana.chainId, rpc)
                        }
                    }
                }
                RxBus.publish(WCChangeEvent())
            } else if (response.errorCode == 401) {
                userRepo.getBotPublicKey(WEB3_BOT_USER_ID, true)
            } else {
                delay(3000)
                jobManager.addJobInBackground(RefreshDappJob())
            }
        }
}
