package one.mixin.android.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttachmentResponse(val attachment_id: String, val upload_url: String?, val view_url: String?, val created_at: String)
