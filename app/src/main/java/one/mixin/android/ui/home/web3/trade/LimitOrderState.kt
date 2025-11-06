package one.mixin.android.ui.home.web3.trade

import android.content.Context
import one.mixin.android.R

enum class LimitOrderState {
    CREATED,
    PRICING,
    QUOTING,
    SETTLED,
    FAILED,
    CANCELLED,
    EXPIRED,
    SUCCESS,
    PENDING,
    UNKNOWN;

    companion object {
        fun from(raw: String?): LimitOrderState {
            val s = raw?.lowercase()?.trim()
            return when (s) {
                "created" -> CREATED
                "pricing" -> PRICING
                "quoting" -> QUOTING
                "settled", "success" -> SETTLED
                "failed" -> FAILED
                "cancelled", "canceled" -> CANCELLED
                "expired" -> EXPIRED
                "pending" -> PENDING
                else -> UNKNOWN
            }
        }
    }

    fun format(context: Context): String = when (this) {
        CREATED -> context.getString(R.string.State_Created)
        PRICING, QUOTING, PENDING -> context.getString(R.string.State_Pending)
        EXPIRED -> context.getString(R.string.Expired)
        SETTLED -> context.getString(R.string.State_Success)
        CANCELLED -> context.getString(R.string.Canceled)
        FAILED -> context.getString(R.string.State_Failed)
        SUCCESS -> context.getString(R.string.State_Success)
        UNKNOWN -> ""
    }
}
