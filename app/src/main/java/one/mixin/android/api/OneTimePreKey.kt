package one.mixin.android.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class OneTimePreKey(
    @Json(name = "key_id")
    val keyId: Int,
    @Json(name = "pub_key")
    val pubKey: String?
)
