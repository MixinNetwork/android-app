package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.api.response.Metadata

@JsonClass(generateAdapter = true)
data class NonFungibleToken(
    val type: String,
    @Json(name = "token_id")
    val tokenId: String,
    @Json(name = "group")
    val groupKey: String,
    @Json(name = "token")
    val tokenKey: String,
    @Json(name = "meta")
    val metadata: Metadata,
    @Json(name ="created_at")
    val createdAt: String
)
