package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper

@JsonClass(generateAdapter = true)
data class ContactMessagePayload(
    @Json(name = "user_id")
    val userId: String
)

fun ContactMessagePayload.toJson(): String =
    MoshiHelper.getTypeAdapter<ContactMessagePayload>(ContactMessagePayload::class.java).toJson(this)
