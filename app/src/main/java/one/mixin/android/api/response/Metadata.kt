package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class Metadata(
    @SerializedName("group")
    val groupName: String,
    @SerializedName("name")
    val tokenName: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("media_url")
    val mediaUrl: String,
    @SerializedName("mime")
    val mime: String,
    @SerializedName("hash")
    val hash: String,
)
