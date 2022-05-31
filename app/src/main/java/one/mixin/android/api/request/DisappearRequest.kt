package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class DisappearRequest(
    @SerializedName("duration")
    val duration: Long
)
