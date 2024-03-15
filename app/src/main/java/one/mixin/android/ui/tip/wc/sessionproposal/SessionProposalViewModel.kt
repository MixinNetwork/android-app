package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import javax.inject.Inject

@HiltViewModel
class SessionProposalViewModel
    @Inject
    internal constructor() : ViewModel() {
        fun rejectSession(
            version: WalletConnect.Version,
            topic: String,
        ) {
            when (version) {
                WalletConnect.Version.V2 -> {
                    WalletConnectV2.rejectSession(topic)
                }
                WalletConnect.Version.TIP -> {}
            }
        }

        fun getSessionProposalUI(
            version: WalletConnect.Version,
            chain: Chain,
            sessionProposal: Wallet.Model.SessionProposal?,
        ): SessionProposalUI? {
            when (version) {
                WalletConnect.Version.V2 -> {
                    if (sessionProposal == null) return null
                    return SessionProposalUI(
                        peer =
                            PeerUI(
                                icon = sessionProposal.icons.firstOrNull().toString(),
                                name = sessionProposal.name,
                                desc = sessionProposal.description,
                                uri = sessionProposal.url.toUri().host ?: "",
                                account = ""
                            ),
                        chain = chain,
                    )
                }
                WalletConnect.Version.TIP -> {
                    return WalletConnectTIP.getSessionProposalUI()
                }
            }
        }
    }
