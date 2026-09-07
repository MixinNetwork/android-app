package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class AttachmentResponse(
    @SerializedName("attachment_id")
    val attachment_id: String,
    @SerializedName("upload_url")
    val upload_url: String?,
    @SerializedName("view_url")
    val view_url: String?,
    @SerializedName("created_at")
    val created_at: String,
)
