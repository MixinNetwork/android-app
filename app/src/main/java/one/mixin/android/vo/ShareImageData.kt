package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class ShareImageData(
    val url: String,
    @SerializedName("attachment_content")
    val attachmentContent: String? = null,
)
