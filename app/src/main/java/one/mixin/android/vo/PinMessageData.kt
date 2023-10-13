package one.mixin.android.vo

data class PinMessageData(
    val messageId: String,
    val conversationId: String,
    val type: String,
    val content: String?,
    val createdAt: String,
)
