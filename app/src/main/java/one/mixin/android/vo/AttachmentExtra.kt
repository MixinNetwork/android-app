package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
class AttachmentExtra(
    @Json(name = "attachment_id")
    var attachmentId: String,
    @Json(name = "message_id")
    var messageId: String?,
    @Json(name = "created_at")
    var createdAt: String? = null,
)

fun AttachmentExtra.toJson(): String =
    getTypeAdapter<AttachmentExtra>(AttachmentExtra::class.java).toJson(this)
