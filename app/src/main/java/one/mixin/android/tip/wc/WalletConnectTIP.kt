package one.mixin.android.tip.wc

import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import one.mixin.android.ui.tip.wc.sessionproposal.SessionProposalUI
import one.mixin.android.ui.tip.wc.sessionrequest.SessionRequestUI

object WalletConnectTIP : WalletConnect() {
    const val TAG = "WalletConnectTIP"

    val peer = PeerUI(
        icon = "",
        name = "TIP Wallet",
        desc = "TIP Wallet Description",
        uri = "7000101002",
    )

    val sessionProposalUI = SessionProposalUI(
        peer = peer,
        chain = chain,
    )

    fun getSessionRequestUI() = SessionRequestUI(
        peerUI = peer,
        requestId = 0L,
        data = currentSignData?.signMessage,
        chain = chain,
    )
}
