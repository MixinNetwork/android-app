package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TipSignRequest(
    @SerializedName("signature")
    val signature: String,
    @SerializedName("identity")
    val identity: String,
    @SerializedName("data")
    val data: String,
)

class TipSignData(
    @SerializedName("identity")
    val identity: String,
    @SerializedName("assignee")
    val assignee: String? = null,
    @SerializedName("ephemeral")
    val ephemeral: String,
    @SerializedName("grace")
    val grace: String,
    @SerializedName("nonce")
    val nonce: String,
    @SerializedName("rotate")
    val rotate: String? = null,
)
