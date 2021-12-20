package one.mixin.android.vo

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TurnServer(
    val url: String,
    val username: String,
    val credential: String
)
