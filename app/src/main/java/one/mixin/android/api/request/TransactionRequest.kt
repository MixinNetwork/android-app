package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TransactionRequest(
    @SerializedName("user_id")
    val userId: String,
    val raw: String
)
