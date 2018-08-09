package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.FloodMessage

@Dao
interface FloodMessageDao : BaseDao<FloodMessage> {

    @Query("SELECT * FROM flood_messages ORDER BY created_at ASC limit 10")
    fun findFloodMessages(): List<FloodMessage>?

    @Query("select count(1) from flood_messages")
    fun getFloodMessageCount(): LiveData<Int>
}