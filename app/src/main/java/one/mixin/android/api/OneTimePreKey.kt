package one.mixin.android.api

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json

open class OneTimePreKey(
    @SerializedName("key_id")
    val keyId: Int,
    @SerializedName("pub_key")
    val pubKey: String?
)
