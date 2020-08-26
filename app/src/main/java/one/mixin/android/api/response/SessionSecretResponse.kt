package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class SessionSecretResponse(
    @SerializedName("server_public_key")
    val serverPublicKey: String
)
