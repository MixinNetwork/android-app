package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import one.mixin.android.util.GsonHelper

class AttachmentContent(
    @SerializedName("attachment_id")
    var attachmentId: String,
    @SerializedName("message_id")
    var messageId: String?,
    @SerializedName("created_at")
    var createdAt: String? = null,
)

fun String.toAttachmentContent(): AttachmentContent? =
    GsonHelper.customGson.fromJson(this, AttachmentContent::class.java)
