package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class GhostKeyRequest(
    @SerializedName("receivers")
    val receivers: List<String>,
    @SerializedName("index")
    val index: Int,
    @SerializedName("hint")
    val hint: String,
)