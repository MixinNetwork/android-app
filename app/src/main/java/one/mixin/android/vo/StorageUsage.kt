package one.mixin.android.vo

import android.arch.persistence.room.Entity

@Entity
data class StorageUsage(
    val conversationId: String,
    val category: String,
    val mediaSize: Long,
    val count: Long)