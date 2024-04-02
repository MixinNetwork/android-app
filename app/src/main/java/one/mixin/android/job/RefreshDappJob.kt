package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.tip.wc.WCUnlockEvent
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

    override fun onRun(): Unit = runBlocking {
        val response = tipService.dapps()
        if (response.isSuccess && response.data != null) {
            val gson = GsonHelper.customGson
            val chainDapp = response.data!!
            chainDapp.forEach {
                when(it.chainId) {
                    Chain.Ethereum.assetId -> {
                        MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.Ethereum.chainId}", gson.toJson(it.dapps))
                        MixinApplication.appContext.defaultSharedPreferences.putString(Chain.Ethereum.chainId, it.rpc)
                    }
                    Chain.BinanceSmartChain.assetId -> {
                        MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.BinanceSmartChain.chainId}", gson.toJson(it.dapps))
                        MixinApplication.appContext.defaultSharedPreferences.putString(Chain.BinanceSmartChain.chainId, it.rpc)
                    }
                    Chain.Polygon.assetId -> {
                        MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.Polygon.chainId}", gson.toJson(it.dapps))
                        MixinApplication.appContext.defaultSharedPreferences.putString(Chain.Polygon.chainId, it.rpc)
                    }
                    Chain.Solana.assetId-> {
                        MixinApplication.appContext.defaultSharedPreferences.putString("dapp_${Chain.Solana.chainId}", gson.toJson(it.dapps))
                        MixinApplication.appContext.defaultSharedPreferences.putString(Chain.Solana.chainId, it.rpc)
                    }
                }
            }
            RxBus.publish(WCUnlockEvent())
        } else {
            jobManager.addJobInBackground(RefreshDappJob())
        }
    }
}
