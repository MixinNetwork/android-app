package one.mixin.android.ui.home.web3

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ConnectionUI
import one.mixin.android.vo.Dapp
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel
@Inject
internal constructor() : ViewModel() {
        fun disconnect(
            version: WalletConnect.Version,
            topic: String,
        ) {
            when (version) {
                WalletConnect.Version.V2 -> {
                    WalletConnectV2.disconnect(topic)
                }
                WalletConnect.Version.TIP -> {}
                WalletConnect.Version.BROWSER -> {}
            }
        }

        fun getLatestActiveSignSessions(): List<ConnectionUI> {
            val v2List =
                WalletConnectV2.getListOfActiveSessions().mapIndexed { index, wcSession ->
                    ConnectionUI(
                        index = index,
                        icon = wcSession.metaData?.icons?.firstOrNull(),
                        name = wcSession.metaData!!.name.takeIf { it.isNotBlank() } ?: "Dapp",
                        uri = wcSession.metaData!!.url.takeIf { it.isNotBlank() } ?: "Not provided",
                        data = wcSession.topic,
                    )
                }
            return v2List
        }

        fun dapps(chainId: String): List<Dapp> {
            val gson = GsonHelper.customGson
            val dapps = MixinApplication.get().defaultSharedPreferences.getString("dapp_$chainId", null)
            if (dapps == null) {
                return emptyList<Dapp>()
            } else {
                return gson.fromJson(dapps, Array<Dapp>::class.java).toList()
            }
        }
    }
