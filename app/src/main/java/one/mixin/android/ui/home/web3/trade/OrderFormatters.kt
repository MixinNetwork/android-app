package one.mixin.android.ui.home.web3.trade

import android.content.Context
import one.mixin.android.R

// String-based formatter for unified Order.limit state values
fun formatLimitOrderState(context: Context, state: String): String {
    return when (state.lowercase()) {
        "created" -> context.getString(R.string.State_Created)
        "pricing", "quoting" -> context.getString(R.string.State_Pending)
        "expired" -> context.getString(R.string.Expired)
        "settled" -> context.getString(R.string.State_Success)
        "cancelled" -> context.getString(R.string.Canceled)
        "failed" -> context.getString(R.string.State_Failed)
        else -> state
    }
}
