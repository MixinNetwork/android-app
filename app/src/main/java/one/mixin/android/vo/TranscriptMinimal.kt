package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TranscriptMinimal(
    val name: String,
    @SerializedName("category")
    @Json(name = "category")
    override val type: String,
    val content: String?
) : ICategory
