package one.mixin.android.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.crypto.vo.Session
import one.mixin.android.db.BaseDao

@Dao
interface SessionDao : BaseDao<Session> {
    @Transaction
    @Query("SELECT * FROM sessions WHERE address = :address AND device = :device")
    fun getSession(
        address: String,
        device: Int,
    ): Session?

    @Transaction
    @Query("SELECT device from sessions WHERE address = :address AND device != 1")
    fun getSubDevice(address: String): List<Int>

    @Transaction
    @Query("SELECT * FROM sessions WHERE address = :address")
    fun getSessions(address: String): List<Session>

    @Query("DELETE FROM sessions WHERE address = :address")
    fun deleteSession(address: String)

    // TODO deprecated
    @Query("SELECT * FROM sessions WHERE device = 1")
    suspend fun syncGetSessionAddress(): List<Session>
}
