package one.mixin.android.ui.tip.wc.connections

import one.mixin.android.tip.wc.Chain

data class ConnectionUI(
    val index: Int,
    val data: String,
    val name: String,
    val uri: String,
    val icon: String?,

    // only v1 for now
    val chain: Chain? = null,
)
