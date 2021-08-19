package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class PinMessageMinimal(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("category")
    override val type: String,
    val content: String?
) : ICategory
