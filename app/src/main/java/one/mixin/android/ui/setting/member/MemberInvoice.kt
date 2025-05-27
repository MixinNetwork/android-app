package one.mixin.android.ui.setting.member

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.vo.Plan

enum class InvoiceStatus {
    EXPIRED, COMPLETED
}

enum class InvoiceType {
    RRNEW, UPGRADE, PURCHASE
}

fun MemberOrder.getInvoiceStatus(): InvoiceStatus {
    return if (status == "success") InvoiceStatus.COMPLETED else InvoiceStatus.EXPIRED
}

fun MemberOrder.getInvoiceType(): InvoiceType {
    return when {
        reason.contains("upgrade", true) -> InvoiceType.UPGRADE
        reason.contains("renew", true) -> InvoiceType.RRNEW
        else -> InvoiceType.PURCHASE
    }
}
