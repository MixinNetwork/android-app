package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class StorageUsage(
    val conversationId: String,
    val category: String,
    val mediaSize: Long,
    val count: Long
)