package one.mixin.android.ui.tip.wc.sessionrequest

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV1
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import javax.inject.Inject

@HiltViewModel
class SessionRequestViewModel @Inject internal constructor() : ViewModel() {

    fun rejectRequest(version: WalletConnect.Version, id: Long, msg: String? = null) {
        when (version) {
            WalletConnect.Version.V1 -> {
                WalletConnectV1.rejectRequest(id)
            }
            WalletConnect.Version.V2 -> {
                WalletConnectV2.rejectRequest(msg)
            }
            WalletConnect.Version.TIP -> {}
        }
    }

    fun getSessionRequestUI(version: WalletConnect.Version): SessionRequestUI<*>? {
        when (version) {
            WalletConnect.Version.V1 -> {
                val signData = WalletConnectV1.currentSignData ?: return null
                val session = WalletConnectV1.currentSession ?: return null
                val peer = session.remotePeerMeta
                val peerUI = PeerUI(
                    uri = peer.url.toUri().host ?: "",
                    name = peer.name,
                    desc = peer.description ?: "",
                    icon = peer.icons.firstOrNull().toString(),
                )
                return SessionRequestUI(
                    peerUI = peerUI,
                    requestId = signData.requestId,
                    data = signData.signMessage,
                    chain = WalletConnectV1.chain,
                )
            }
            WalletConnect.Version.V2 -> {
                val signData = (WalletConnectV2.currentSignData ?: return null) as? WalletConnect.WCSignData.V2SignData ?: return null
                val sessionRequest = signData.sessionRequest
                val peer = sessionRequest.peerMetaData ?: return null
                val peerUI = PeerUI(
                    name = peer.name,
                    icon = peer.icons.firstOrNull() ?: "",
                    uri = peer.url.toUri().host ?: "",
                    desc = peer.description,
                )
                return SessionRequestUI(
                    peerUI = peerUI,
                    requestId = signData.requestId,
                    data = signData.signMessage,
                    chain = WalletConnectV2.chain,
                )
            }
            WalletConnect.Version.TIP -> {
                return WalletConnectTIP.getSessionRequestUI()
            }
        }
    }

    fun <T> getContent(
        version: WalletConnect.Version,
        gson: Gson,
        data: T,
    ): String = when (version) {
        WalletConnect.Version.V1 -> {
            when (data) {
                is WCEthereumSignMessage -> {
                    gson.toJson(data.data)
                }
                is WCEthereumTransaction -> {
                    gson.toJson(data)
                }
                else -> "Invalid data"
            }
        }
        WalletConnect.Version.V2 -> {
            gson.toJson(data)
        }
        WalletConnect.Version.TIP -> {
            data as String
        }
    }
}
