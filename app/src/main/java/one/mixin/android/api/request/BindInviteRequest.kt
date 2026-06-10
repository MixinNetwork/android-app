package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class BindInviteRequest(
    @SerializedName("code") val code: String
)
