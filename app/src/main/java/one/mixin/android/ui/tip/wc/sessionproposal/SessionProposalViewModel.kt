package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.Constants
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import javax.inject.Inject

@HiltViewModel
class SessionProposalViewModel
    @Inject
    internal constructor() : ViewModel() {
        private var account: String = ""

        suspend fun init() {
            account = PropertyHelper.findValueByKey(Constants.Account.PREF_WALLET_CONNECT_ADDRESS, "")
        }

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
                                account = account,
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
