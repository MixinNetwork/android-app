package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class WithdrawalRequest(
    @SerializedName("address_id")
    val addressId: String?,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("pin")
    val pin: String,
    @SerializedName("trace_id")
    val traceId: String,
    @SerializedName("memo")
    val memo: String?,
    @SerializedName("fee")
    val fee: String?,
    @SerializedName("asset_id")
    val assetId: String?,
    @SerializedName("destination")
    val destination: String?,
    @SerializedName("tag")
    val tag: String?,
)
