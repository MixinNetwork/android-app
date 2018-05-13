package one.mixin.android.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "messages_history")
class MessageHistory(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String
//        @ColumnInfo(name = "category")
//        val category: String,
//        @ColumnInfo(name = "status")
//        val status: String
)
