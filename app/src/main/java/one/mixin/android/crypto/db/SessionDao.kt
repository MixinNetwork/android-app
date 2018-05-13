package one.mixin.android.crypto.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import one.mixin.android.crypto.vo.Session
import one.mixin.android.db.BaseDao

@Dao
interface SessionDao : BaseDao<Session> {

    @Transaction
    @Query("SELECT * FROM sessions WHERE address = :address AND device = :device")
    fun getSession(address: String, device: Int): Session?

    @Transaction
    @Query("SELECT device from sessions where address = :address AND device != 1")
    fun getSubDevice(address: String): List<Int>?

    @Transaction
    @Query("SELECT * FROM sessions where address = :address")
    fun getSessions(address: String): List<Session>?
}