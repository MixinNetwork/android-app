package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class SessionSecretResponse(
    @SerializedName("pin_token")
    val pinToken: String
)
