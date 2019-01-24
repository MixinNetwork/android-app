package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Session

@Dao
interface SessionDao : BaseDao<Session> {
    @Query("SELECT * FROM sessions WHERE user_id = :userId")
    fun findSessionByUserId(userId: String): List<Session>?

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND device_id != 1")
    fun findSecondarySessionByUserId(userId: String): List<Session>?

    @Query("DELETE FROM sessions WHERE user_id= :userId")
    fun deleteByUserId(userId: String)
}
