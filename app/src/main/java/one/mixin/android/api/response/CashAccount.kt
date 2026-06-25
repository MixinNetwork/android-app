package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class CashAccount(
    @SerializedName("balance")
    val balance: String,
    @SerializedName("min_amount")
    val minAmount: String,
)
