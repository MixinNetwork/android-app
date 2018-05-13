package one.mixin.android.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import one.mixin.android.vo.Offset

@Dao
interface OffsetDao : BaseDao<Offset> {

    @Transaction
    @Query("SELECT timestamp FROM offsets WHERE key = 'messages_status_offset'")
    fun getStatusOffset(): String?
}
