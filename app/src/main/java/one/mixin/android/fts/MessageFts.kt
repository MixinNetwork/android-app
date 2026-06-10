package one.mixin.android.fts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Entity(tableName = "messages_fts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
class MessageFts(
    @ColumnInfo(name = "content", typeAffinity = ColumnInfo.TEXT)
    val content: String,
)
