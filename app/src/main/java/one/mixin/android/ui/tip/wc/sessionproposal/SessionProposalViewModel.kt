package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV1
import one.mixin.android.tip.wc.WalletConnectV2
import javax.inject.Inject

@HiltViewModel
class SessionProposalViewModel @Inject internal constructor() : ViewModel() {

    fun rejectSession(version: WalletConnect.Version) {
        when (version) {
            WalletConnect.Version.V1 -> { WalletConnectV1.rejectSession() }
            WalletConnect.Version.V2 -> { WalletConnectV2.rejectSession() }
        }
    }

    fun getSessionProposalUI(version: WalletConnect.Version): PeerUI? {
        when (version) {
            WalletConnect.Version.V1 -> {
                val session = WalletConnectV1.getLastSession() ?: return null
                val peer = session.remotePeerMeta
                return PeerUI(
                    uri = peer.url,
                    name = peer.name,
                    desc = peer.description ?: "",
                    icon = peer.icons.firstOrNull().toString(),
                )
            }
            WalletConnect.Version.V2 -> {
                val sessionProposal = WalletConnectV2.getSessionProposals().lastOrNull()
                return if (sessionProposal != null) {
                    PeerUI(
                        icon = sessionProposal.icons.firstOrNull().toString(),
                        name = sessionProposal.name,
                        desc = sessionProposal.description,
                        uri = sessionProposal.url,
                    )
                } else {
                    null
                }
            }
        }
    }
}
