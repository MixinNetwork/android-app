package one.mixin.android.ui.tip.wc.sessionrequest

import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI

data class SessionRequestUI(
    val peerUI: PeerUI,
    val requestId: Long,
    val param: String,
    val chain: String? = null,
    val method: String? = null,
)
