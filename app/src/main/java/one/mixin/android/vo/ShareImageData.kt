package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class ShareImageData(
    val url: String,
    @SerializedName("attachment_extra")
    val attachmentExtra: String? = null,
)
