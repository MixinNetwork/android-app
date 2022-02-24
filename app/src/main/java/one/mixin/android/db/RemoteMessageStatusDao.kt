package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.RemoteMessageStatus

@Dao
interface RemoteMessageStatusDao : BaseDao<RemoteMessageStatus> {
    @Query("SELECT * FROM remote_messages_status LIMIT 100")
    fun findRemoteMessageStatus(): List<RemoteMessageStatus>
}
