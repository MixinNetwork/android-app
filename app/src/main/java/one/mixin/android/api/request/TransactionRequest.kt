package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class TransactionRequest(
    val raw: String,
    @SerializedName("request_id")
    val requestId: String,
)
