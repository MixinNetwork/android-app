package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class TranscriptMinimal(
    val name: String,
    @SerializedName("category")
    override val type: String,
    val content: String?
) : ICategory
