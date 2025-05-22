package one.mixin.android.ui.setting.star

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.vo.Plan

@Parcelize
data class MemberInvoice(
    val plan: Plan,
    val transactionId: String,
    val via: String,
    val amount: String,
    val description: String,
    val time: String,
    val status: InvoiceStatus,
    val type: InvoiceType
) : Parcelable

enum class InvoiceStatus {
    EXPIRED, COMPLETED
}

enum class InvoiceType {
    RRNEW, UPGRADE, PURCHASE
}