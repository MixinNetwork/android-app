package one.mixin.android.vo.route

import android.content.Context
import one.mixin.android.R

enum class OrderState(val value: String) {
    CREATED("created"),
    PRICING("pricing"),
    QUOTING("quoting"),
    PENDING("pending"),
    SUCCESS("success"),
    SETTLED("settled"),
    FAILED("failed"),
    EXPIRED("expired"),
    CANCELLED("cancelled"),
    REFUNDED("refunded"),
    CANCELLING("cancelling");

    companion object {
        fun from(raw: String?): OrderState {
            return when (raw?.lowercase()) {
                CREATED.value -> CREATED
                PRICING.value -> PRICING
                QUOTING.value -> QUOTING
                PENDING.value -> PENDING
                SUCCESS.value, SETTLED.value, "settled" -> SETTLED
                FAILED.value -> FAILED
                EXPIRED.value -> EXPIRED
                CANCELLED.value, "canceled" -> CANCELLED
                REFUNDED.value -> REFUNDED
                CANCELLING.value -> CANCELLING
                else -> PENDING
            }
        }
    }

    fun format(context: Context): String = when (this) {
        CREATED -> context.getString(R.string.State_Created)
        PRICING, QUOTING, PENDING -> context.getString(R.string.State_Pending)
        SUCCESS, SETTLED -> context.getString(R.string.State_Success)
        FAILED -> context.getString(R.string.State_Failed)
        EXPIRED -> context.getString(R.string.Expired)
        CANCELLED -> context.getString(R.string.Canceled)
        REFUNDED -> context.getString(R.string.State_Refunded)
        CANCELLING -> context.getString(R.string.order_state_cancelling)
    }
}
