package one.mixin.android.ui.tip.wc.sessionrequest

import androidx.lifecycle.ViewModel
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.tip.wc.WalletConnect
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
        }
    }

    fun getSessionRequestUI(version: WalletConnect.Version): SessionRequestUI<*>? {
        when (version) {
            WalletConnect.Version.V1 -> {
                val signData = WalletConnectV1.currentSignData ?: return null
                val session = WalletConnectV1.getLastSession() ?: return null
                val peer = session.remotePeerMeta
                val peerUI = PeerUI(
                    uri = peer.url,
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
                    uri = peer.url,
                    desc = peer.description,
                )
                return SessionRequestUI(
                    peerUI = peerUI,
                    requestId = signData.requestId,
                    data = signData.signMessage,
                    chain = WalletConnectV2.chain,
                )
            }
        }
    }

    fun <T> getContent(
        version: WalletConnect.Version,
        data: T,
    ): String = when (version) {
        WalletConnect.Version.V1 -> {
            when (data) {
                is WCEthereumSignMessage -> {
                    data.data
                }
                is WCEthereumTransaction -> {
                    data.toString()
                }
                else -> "Invalid data"
            }
        }
        WalletConnect.Version.V2 -> {
            data.toString()
        }
    }
}
