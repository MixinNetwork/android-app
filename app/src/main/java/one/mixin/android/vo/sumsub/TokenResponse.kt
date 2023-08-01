package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

class TokenResponse(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("state")
    val state: State
)

enum class State {
    @SerializedName("pending")
    PENDING,

    @SerializedName("success")
    SUCCESS
}