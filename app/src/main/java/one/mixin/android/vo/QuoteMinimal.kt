package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class QuoteMinimal(
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "quote_message_id")
    val quoteMessageId: String,
)
