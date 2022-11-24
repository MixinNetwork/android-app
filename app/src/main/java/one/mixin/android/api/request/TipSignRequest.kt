package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TipSignRequest(
    @SerializedName("signature")
    val signature: String,
    @SerializedName("identity")
    val identity: String,
    @SerializedName("data")
    val data: String,
    @SerializedName("watcher")
    val watcher: String,
    @SerializedName("action")
    val action: String = "SIGN"
)

class TipSignData(
    @SerializedName("identity")
    val identity: String,
    @SerializedName("assignee")
    val assignee: String? = null,
    @SerializedName("ephemeral")
    val ephemeral: String,
    @SerializedName("watcher")
    val watcher: String,
    @SerializedName("grace")
    val grace: Long,
    @SerializedName("nonce")
    val nonce: Long,
    @SerializedName("rotate")
    val rotate: String? = null
)
