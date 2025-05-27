package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import java.util.Date
data class MemberOrder(
    @SerializedName("type")
    val type: String,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount_original")
    val amountOriginal: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("amount_actual")
    val amountActual: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("before")
    val before: String,
    @SerializedName("after")
    val after: String,
    @SerializedName("quantity_accounts")
    val quantityAccounts: Long,
    @SerializedName("quantity_transactions")
    val quantityTransactions: Long,
    @SerializedName("method")
    val method: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("paid_at")
    val paidAt: Date,
    @SerializedName("expired_at")
    val expiredAt: Date,
    @SerializedName("created_at")
    val createdAt: Date,
    @SerializedName("payment_url")
    val paymentUrl: String?,
    @SerializedName("fiat_order")
    val fiatOrder: FiatOrderView?,
    @SerializedName("checkout_user_id")
    val checkoutUserId: String,
    @SerializedName("checkout_memo")
    val checkoutMemo: String
)

data class FiatOrderView(
    @SerializedName("type")
    val type: String,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("subscription_id")
    val subscriptionId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("receipt")
    val receipt: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("paid_at")
    val paidAt: Date,
    @SerializedName("created_at")
    val createdAt: Date
)
