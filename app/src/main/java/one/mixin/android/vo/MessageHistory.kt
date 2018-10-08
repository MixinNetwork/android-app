package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
