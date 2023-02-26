package one.mixin.android.ui.tip.wc.sessionrequest

import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI

data class SessionRequestUI<T>(
    val peerUI: PeerUI,
    val requestId: Long,
    val data: T,
    val chain: String? = null,
    val method: String? = null,
)
