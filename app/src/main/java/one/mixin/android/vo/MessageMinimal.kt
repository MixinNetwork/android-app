package one.mixin.android.vo

import android.arch.persistence.room.Entity

@Entity
data class MessageMinimal(
    val id: String,
    val created_at: String
)