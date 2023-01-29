package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.FloodMessage

@Dao
interface FloodMessageDao : BaseDao<FloodMessage> {

    @Query("SELECT * FROM flood_messages ORDER BY created_at ASC limit 10")
    suspend fun findFloodMessages(): List<FloodMessage>

    @Query("SELECT * FROM flood_messages ORDER BY created_at ASC limit 100")
    fun limit100(): List<FloodMessage>

    @Query("select count(1) from flood_messages")
    fun getFloodMessageCount(): LiveData<Int>

    @Query("select created_at from flood_messages ORDER BY created_at DESC limit 1")
    fun getLastBlazeMessageCreatedAt(): String?

    @Query(
        """
        DELETE FROM flood_messages 
        WHERE message_id = (
            SELECT message_id FROM flood_messages 
            WHERE message_id in (:ids) 
            ORDER BY length(data) DESC LIMIT 1
        )
    """,
    )
    suspend fun deleteMaxLenMessage(ids: List<String>)

    @Query("SELECT message_id FROM flood_messages ORDER BY created_at ASC limit 10")
    suspend fun findMessageIdsLimit10(): List<String>
}
