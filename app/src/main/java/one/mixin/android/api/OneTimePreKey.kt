package one.mixin.android.api

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class OneTimePreKey(
    @SerializedName("key_id")
    @Json(name = "key_id")
    val keyId: Int,
    @SerializedName("pub_key")
    @Json(name = "pub_key")
    val pubKey: String?
)
