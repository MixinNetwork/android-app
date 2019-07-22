package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class SignalKeyCount(
    @SerializedName("one_time_pre_keys_count")
    val preKeyCount: Int
)
