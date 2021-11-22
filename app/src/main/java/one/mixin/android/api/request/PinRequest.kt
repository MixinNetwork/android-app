package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PinRequest(
    @Json(name ="pin")
    val pin: String,
    @Json(name ="old_pin")
    val oldPin: String? = null
)
