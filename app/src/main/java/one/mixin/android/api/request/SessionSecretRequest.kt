package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class SessionSecretRequest(
    @SerializedName("session_secret")
    val sessionSecret: String
)
