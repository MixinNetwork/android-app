package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class ContactMessagePayload(
    @Json(name = "user_id")
    val userId: String
)

fun ContactMessagePayload.toJson(): String =
    getTypeAdapter<ContactMessagePayload>(ContactMessagePayload::class.java).toJson(this)
