package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class RegisterResponse(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("created_at")
    val createdAt: String,
)