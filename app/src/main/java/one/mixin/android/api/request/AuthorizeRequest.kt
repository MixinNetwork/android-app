package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class AuthorizeRequest(
    @SerializedName("authorization_id")
    val authorizationId: String,
    @SerializedName("scopes")
    val scopes: List<String>,
    @SerializedName("pin_base64")
    val pin: String?,
)
