package one.mixin.android.ui.tip.wc.sessionrequest

import androidx.lifecycle.ViewModel
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

    fun getSessionRequestUI(version: WalletConnect.Version): SessionRequestUI? {
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
                    param = signData.signMessage.data,
                )
            }
            WalletConnect.Version.V2 -> {
                val sessionRequest = WalletConnectV2.sessionRequest ?: return null
                return SessionRequestUI(
                    peerUI = PeerUI(
                        name = sessionRequest.peerMetaData?.name ?: "",
                        icon = sessionRequest.peerMetaData?.icons?.firstOrNull() ?: "",
                        uri = sessionRequest.peerMetaData?.url ?: "",
                        desc = sessionRequest.peerMetaData?.description ?: "",
                    ),
                    requestId = sessionRequest.request.id,
                    param = sessionRequest.request.params,
                    chain = sessionRequest.chainId,
                    method = sessionRequest.request.method,
                )
            }
        }
    }
}
