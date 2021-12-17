package one.mixin.android.api.request

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CollectibleRequest(
    val action: String,
    val raw: String,
    val pin: String
)
