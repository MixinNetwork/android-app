package one.mixin.android.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Metadata(
    @Json(name ="group")
    val groupName: String,
    @Json(name ="name")
    val tokenName: String,
    val description: String,
    @Json(name ="icon_url")
    val iconUrl: String,
    @Json(name ="media_url")
    val mediaUrl: String,
    val mime: String,
    val hash: String,
)
