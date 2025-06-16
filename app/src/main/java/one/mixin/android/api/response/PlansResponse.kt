package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class MemberPlan(
    @SerializedName("plans")
    val plans: List<MemberOrderPlan>,
    @SerializedName("transaction")
    val transaction: Transaction
)

data class MemberOrderPlan(
    @SerializedName("name")
    val name: String,
    @SerializedName("plan")
    val plan: String,
    @SerializedName("account_quota")
    val accountQuota: Int,
    @SerializedName("transaction_quota")
    val transactionQuota: Int,
    @SerializedName("accountants_quota")
    val accountantsQuota: Int,
    @SerializedName("members_quota")
    val membersQuota: Int,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("amount_discount")
    val amountDiscount: String,
    @SerializedName("amount_payment")
    val amountPayment: String,
    @SerializedName("play_store_subscription_id")
    val playStoreSubscriptionId: String?
)

data class Transaction(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("fee")
    val fee: String,
    @SerializedName("recovery_fee")
    val recoveryFee: String
)
