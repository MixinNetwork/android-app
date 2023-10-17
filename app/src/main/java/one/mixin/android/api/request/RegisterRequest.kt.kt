package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.session.Session

data class RegisterRequest(
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("user_id")
    val user: String = Session.getAccountId()!!
)
