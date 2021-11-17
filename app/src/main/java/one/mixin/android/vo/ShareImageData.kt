package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper

@JsonClass(generateAdapter = true)
data class ShareImageData(
    val url: String,
    @Json(name = "attachment_extra")
    val attachmentExtra: String? = null,
)

fun ShareImageData.toJson(): String =
    MoshiHelper.getTypeAdapter<ShareImageData>(ShareImageData::class.java).toJson(this)
