package one.mixin.android.ui.home.web3

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.TipService
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.vo.ChainDapp
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


        fun dapps(): MutableList<ChainDapp>  = MixinApplication.get().chainDapp
    }
