package one.mixin.android.vo.route

import android.content.Context
import one.mixin.android.R

enum class OrderState(val value: String) {
    CREATED("created"),
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed"),
    REFUNDED("refunded");

    companion object {
        fun from(raw: String?): OrderState {
            return when (raw?.lowercase()) {
                CREATED.value -> CREATED
                PENDING.value -> PENDING
                SUCCESS.value -> SUCCESS
                FAILED.value -> FAILED
                REFUNDED.value -> REFUNDED
                else -> PENDING
            }
        }
    }

    fun format(context: Context): String = when (this) {
        CREATED -> context.getString(R.string.State_Created)
        PENDING -> context.getString(R.string.State_Pending)
        SUCCESS -> context.getString(R.string.State_Success)
        FAILED -> context.getString(R.string.State_Failed)
        REFUNDED -> context.getString(R.string.State_Refunded)
    }
}
