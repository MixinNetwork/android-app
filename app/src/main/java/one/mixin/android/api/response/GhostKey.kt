package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class GhostKey(
    @SerializedName("mask")
    val mask: String,
    @SerializedName("keys")
    val keys: List<String>
)
