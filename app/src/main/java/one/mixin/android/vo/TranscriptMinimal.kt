package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class TranscriptMinimal(
    @SerializedName("name")
    val name: String,
    @SerializedName("category")
    override val type: String,
    @SerializedName("content")
    val content: String?,
) : ICategory
