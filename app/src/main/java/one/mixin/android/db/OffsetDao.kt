package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import one.mixin.android.vo.Offset

@Dao
interface OffsetDao : BaseDao<Offset> {
    @Transaction
    @Query("SELECT timestamp FROM offsets WHERE `key` = 'messages_status_offset'")
    fun getStatusOffset(): String?
}
