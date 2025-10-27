package one.mixin.android.ui.home.web3.trade

import android.content.Context
import one.mixin.android.R
import one.mixin.android.api.response.LimitOrderStatus

// Map LimitOrderStatus to localized user-facing text
fun formatOrderState(context: Context, state: LimitOrderStatus): String {
    return when (state) {
        LimitOrderStatus.CREATED -> context.getString(R.string.State_Created)
        LimitOrderStatus.PRICING, LimitOrderStatus.QUOTING -> context.getString(R.string.State_Pending)
        LimitOrderStatus.EXPIRED -> context.getString(R.string.Expired)
        LimitOrderStatus.SETTLED -> context.getString(R.string.State_Success)
        LimitOrderStatus.CANCELLED -> context.getString(R.string.Canceled)
        LimitOrderStatus.FAILED -> context.getString(R.string.State_Failed)
        LimitOrderStatus.UNKNOWN -> state.value
    }
}

// Alias specifically for LimitOrder to avoid overload ambiguity
fun formatLimitOrderState(context: Context, state: LimitOrderStatus): String =
    formatOrderState(context, state)
