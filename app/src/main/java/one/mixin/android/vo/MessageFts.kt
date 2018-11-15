package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Entity(tableName = "messages_fts")
@Fts4(contentEntity = Message::class, tokenizer = FtsOptions.Tokenizer.UNICODE61)
class MessageFts(
    @ColumnInfo(name = "content")
    var content: String?,
    @ColumnInfo(name = "name")
    var name: String?)