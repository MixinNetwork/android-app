package one.mixin.android.tip.wc

import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import one.mixin.android.ui.tip.wc.sessionproposal.SessionProposalUI
import one.mixin.android.ui.tip.wc.sessionrequest.SessionRequestUI

object WalletConnectTIP : WalletConnect() {
    const val TAG = "WalletConnectTIP"

    var peer: PeerUI? = null

    var signData: WCSignData.TIPSignData? = null

    var walletId: String? = null

    fun clear() {
        peer = null
        signData = null
        walletId = null
    }

    fun getSessionProposalUI(): SessionProposalUI? {
        if (!isCurrentWalletValid()) return null
        val p = peer ?: return null
        return SessionProposalUI(
            peer = p,
            chain = Chain.Ethereum,
        )
    }

    fun getSessionRequestUI(): SessionRequestUI<String?>? {
        if (!isCurrentWalletValid()) return null
        val p = peer ?: return null
        return SessionRequestUI(
            peerUI = p,
            requestId = 0L,
            data = signData?.signMessage,
            chain = Chain.Ethereum,
        )
    }

    private fun isCurrentWalletValid(): Boolean {
        val precheck = checkCurrentSigningWallet()
        if (precheck !is SigningWalletPrecheck.Eligible) return false
        val boundWalletId = walletId
        return boundWalletId == null || boundWalletId == precheck.walletId
    }
}
