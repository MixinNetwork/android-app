package one.mixin.android.vo.route

import android.content.Context
import one.mixin.android.R

enum class OrderState(val value: String) {
    CREATED("created"),
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed"),
    EXPIRED("expired"),
    REFUNDED("refunded"),
    CANCELLING("cancelling");

    companion object {
        fun from(raw: String?): OrderState {
            return when (raw?.lowercase()) {
                CREATED.value -> CREATED
                PENDING.value -> PENDING
                SUCCESS.value -> SUCCESS
                FAILED.value -> FAILED
                REFUNDED.value -> REFUNDED
                CANCELLING.value -> CANCELLING
                else -> PENDING
            }
        }
    }

    fun format(context: Context): String = when (this) {
        CREATED -> context.getString(R.string.State_Created)
        PENDING -> context.getString(R.string.State_Pending)
        SUCCESS -> context.getString(R.string.State_Success)
        FAILED -> context.getString(R.string.State_Failed)
        EXPIRED -> context.getString(R.string.Expired)
        REFUNDED -> context.getString(R.string.State_Refunded)
        CANCELLING -> context.getString(R.string.order_state_cancelling)
    }
}
