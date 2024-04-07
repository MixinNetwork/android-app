package one.mixin.android.ui.tip.wc.sessionrequest

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import javax.inject.Inject

@HiltViewModel
class SessionRequestViewModel
    @Inject
    internal constructor() : ViewModel() {
        private var account: String = ""

        suspend fun init() {
            account = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
        }

        fun rejectRequest(
            version: WalletConnect.Version,
            topic: String,
            msg: String? = null,
        ) {
            when (version) {
                WalletConnect.Version.V2 -> {
                    WalletConnectV2.rejectRequest(msg, topic)
                }
                WalletConnect.Version.TIP -> {}
                WalletConnect.Version.BROWSER -> {}
            }
        }

        fun getSessionRequestUI(
            version: WalletConnect.Version,
            chain: Chain,
            signData: WalletConnect.WCSignData.V2SignData<*>?,
            sessionRequest: Wallet.Model.SessionRequest?,
        ): SessionRequestUI<*>? {
            when (version) {
                WalletConnect.Version.V2 -> {
                    if (signData == null || sessionRequest == null) return null
                    val peer = sessionRequest.peerMetaData ?: return null
                    val peerUI =
                        PeerUI(
                            name = peer.name,
                            icon = peer.icons.firstOrNull() ?: "",
                            uri = peer.url.toUri().host ?: "",
                            desc = peer.description,
                            account = account,
                        )
                    return SessionRequestUI(
                        peerUI = peerUI,
                        requestId = signData.requestId,
                        data = signData.signMessage,
                        chain = chain,
                    )
                }
                WalletConnect.Version.TIP -> {
                    return WalletConnectTIP.getSessionRequestUI()
                }
                WalletConnect.Version.BROWSER -> {
                    return WalletConnectTIP.getSessionRequestUI()
                }
            }
        }

        fun <T> getContent(
            version: WalletConnect.Version,
            gson: Gson,
            data: T,
        ): String =
            when (version) {
                WalletConnect.Version.V2 -> {
                    gson.toJson(data)
                }
                WalletConnect.Version.TIP -> {
                    data as String
                }
                WalletConnect.Version.BROWSER->{
                    // todo
                    ""
                }
            }
    }
