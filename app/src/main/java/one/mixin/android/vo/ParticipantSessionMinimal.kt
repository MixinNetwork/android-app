package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import one.mixin.android.db.converter.ListConverter

@TypeConverters(ListConverter::class)
class ParticipantSessionMinimal(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "app_id")
    val appId: String?,
    @ColumnInfo(name = "capabilities")
    val capabilities: List<String>?,
)
