package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.Offset

@Dao
interface OffsetDao : BaseDao<Offset> {

    @Transaction
    @Query("SELECT timestamp FROM offsets WHERE `key` = 'messages_status_offset'")
    fun getStatusOffset(): String?
}
