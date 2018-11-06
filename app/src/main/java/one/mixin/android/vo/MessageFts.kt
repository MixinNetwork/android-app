package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "messages_fts")
@Fts4(contentEntity = Message::class)
class MessageFts(
    @ColumnInfo(name = "content")
    var content: String?,
    @ColumnInfo(name = "name")
    var name: String?)