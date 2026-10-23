package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity
data class QuoteMinimal(
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "quote_message_id")
    val quoteMessageId: String,
)
