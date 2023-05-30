package one.mixin.android.db.pending

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao

@Dao
interface NotificationExtDao : BaseDao<NotificationExt> {
    @Query("SELECT * FROM notification_ext WHERE message_id = :messageId")
    fun findNotificationExtById(messageId: String): NotificationExt?

    @Query("DELETE FROM notification_ext WHERE message_id = :messageId")
    fun deleteById(messageId: String)
}
