package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.ColumnInfo.Companion.TEXT
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions

@Entity(tableName = "messages_fts4")
@Fts4(notIndexed = ["message_id"], tokenizer = FtsOptions.TOKENIZER_UNICODE61)
class MessageFts4(
    @ColumnInfo(name = "message_id", typeAffinity = TEXT)
    val messageId: String,
    @ColumnInfo(name = "content", typeAffinity = TEXT)
    val content: String?,
)
