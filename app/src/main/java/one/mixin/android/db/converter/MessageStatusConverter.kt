package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.vo.MessageStatus

class MessageStatusConverter {
    @ColumnTypeConverter
    fun revertDate(value: String): MessageStatus {
        return requireNotNull(MessageStatus.values().firstOrNull { it.name == value })
    }

    @ColumnTypeConverter
    fun converterDate(status: MessageStatus) = status.name
}
