package one.mixin.android.vo

data class StorageUsage(
    val conversationId: String,
    val type: String,
    val count: Int,
    val mediaSize: Long
)
