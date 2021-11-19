package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
class AttachmentExtra(
    @SerializedName("attachment_id")
    @Json(name = "attachment_id")
    var attachmentId: String,
    @SerializedName("message_id")
    @Json(name = "message_id")
    var messageId: String?,
    @SerializedName("created_at")
    @Json(name = "created_at")
    var createdAt: String? = null,
)

fun AttachmentExtra.toJson(): String =
    getTypeAdapter<AttachmentExtra>(AttachmentExtra::class.java).toJson(this)
