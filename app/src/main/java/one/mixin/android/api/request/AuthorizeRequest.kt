package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AuthorizeRequest(
    @Json(name = "authorization_id")
    val authorizationId: String,
    val scopes: List<String>
)
