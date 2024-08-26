package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_WEB3_BOT_PK
import one.mixin.android.Constants.RouteConfig.WEB3_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponseException
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.WCChangeEvent
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId

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
            getBotPublicKey()
            val response = web3Service.dapps()
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
                getBotPublicKey()
            } else {
                delay(3000)
                jobManager.addJobInBackground(RefreshDappJob())
            }
        }

    private suspend fun getBotPublicKey() {
        val key =
            participantSessionDao.findBotPublicKey(
                generateConversationId(
                    WEB3_BOT_USER_ID,
                    Session.getAccountId()!!,
                ),
                WEB3_BOT_USER_ID,
            )
        if (key != null) {
            MixinApplication.appContext.defaultSharedPreferences.putString(PREF_WEB3_BOT_PK, key)
        } else {
            val sessionResponse = userService.fetchSessionsSuspend(listOf(WEB3_BOT_USER_ID))
            if (sessionResponse.isSuccess) {
                val sessionData = requireNotNull(sessionResponse.data)[0]
                participantSessionDao.insertSuspend(
                    ParticipantSession(
                        generateConversationId(
                            sessionData.userId,
                            Session.getAccountId()!!,
                        ),
                        sessionData.userId,
                        sessionData.sessionId,
                        publicKey = sessionData.publicKey,
                    ),
                )
                MixinApplication.appContext.defaultSharedPreferences.putString(PREF_WEB3_BOT_PK, sessionData.publicKey)
            } else {
                throw MixinResponseException(
                    sessionResponse.errorCode,
                    sessionResponse.errorDescription,
                )
            }
        }
    }
}
