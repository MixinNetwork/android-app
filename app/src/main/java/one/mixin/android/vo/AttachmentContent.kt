package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class AttachmentContent(
    @SerializedName("attachment_id")
    var attachmentId: String,
    @SerializedName("message_id")
    var messageId: String?,
    @SerializedName("created_at")
    var createdAt: String? = null,
)