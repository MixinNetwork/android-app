package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.vo.MessageStatus

class MessageStatusConverter {
    @TypeConverter
    fun revertDate(value: String): MessageStatus {
        return requireNotNull(MessageStatus.values().firstOrNull { it.name == value })
    }

    @TypeConverter
    fun converterDate(status: MessageStatus) = status.name
}
