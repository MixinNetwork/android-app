package one.mixin.android.fts

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions

@Entity(tableName = "messages_fts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
class MessageFts(
    @ColumnInfo(name = "content", typeAffinity = ColumnInfo.TEXT)
    val content: String,
)
