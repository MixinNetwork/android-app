package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

class TokenResponse(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("state")
    val state: String
)

enum class State(val value:String) {
    PENDING("pending"),

    SUCCESS("success")
}