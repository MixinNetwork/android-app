package one.mixin.android.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignalKeyCount(
    @Json(name = "one_time_pre_keys_count")
    val preKeyCount: Int
)
