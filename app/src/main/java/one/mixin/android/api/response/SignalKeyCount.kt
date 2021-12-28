package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignalKeyCount(
    @SerializedName("one_time_pre_keys_count")
    @Json(name = "one_time_pre_keys_count")
    val preKeyCount: Int
)
