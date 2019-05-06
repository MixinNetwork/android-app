package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Job

@Dao
interface JobDao : BaseDao<Job> {
    @Query("SELECT * FROM jobs WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' ORDER BY created_at ASC LIMIT 100")
    suspend fun findAckJobsSync(): List<Job>?

    @Query("SELECT * FROM jobs WHERE `action` = 'ACKNOWLEDGE_SESSION_MESSAGE_RECEIPTS' ORDER BY created_at ASC LIMIT 100")
    suspend fun findSessionAckJobsSync(): List<Job>?

    @Query("SELECT * FROM jobs WHERE `action` = 'CREATE_SESSION_MESSAGE' ORDER BY created_at ASC LIMIT 100")
    suspend fun findCreatePlainSessionJobsSync(): List<Job>?

    @Query("DELETE FROM jobs WHERE `action` = 'CREATE_SESSION_MESSAGE'")
    fun removeExtensionSessionJob()
}