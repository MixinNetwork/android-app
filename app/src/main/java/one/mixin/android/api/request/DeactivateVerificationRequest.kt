package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class DeactivateVerificationRequest(
    @SerializedName("purpose")
    val purpose: String,
    @SerializedName("code")
    val code: String
)
