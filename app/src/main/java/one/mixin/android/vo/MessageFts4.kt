package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.TEXT
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Entity(tableName = "messages_fts4")
@Fts4(notIndexed = ["message_id"], tokenizer = FtsOptions.TOKENIZER_UNICODE61)
class MessageFts4(
    @ColumnInfo(name = "message_id", typeAffinity = TEXT)
    val messageId: String,
    @ColumnInfo(name = "content", typeAffinity = TEXT)
    val content: String?
)
