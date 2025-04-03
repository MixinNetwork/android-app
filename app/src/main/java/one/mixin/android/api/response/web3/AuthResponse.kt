package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    val address: String,
    @SerializedName("user_id")
    val userId: String,
)