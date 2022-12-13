package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.Constants.ACK_LIMIT
import one.mixin.android.vo.Job

@Dao
interface JobDao : BaseDao<Job> {
    @Query("SELECT * FROM jobs WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' ORDER BY rowid ASC LIMIT $ACK_LIMIT")
    fun findAckJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs ORDER BY rowid ASC LIMIT $ACK_LIMIT")
    fun limit100(): List<Job>

    @Query("SELECT count(1) FROM jobs WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS'")
    suspend fun findAckJobsCount(): Int

    @Query("SELECT * FROM jobs WHERE `action` = 'CREATE_MESSAGE' ORDER BY created_at ASC LIMIT $ACK_LIMIT")
    suspend fun findCreateMessageJobs(): List<Job>

    @Query("SELECT * FROM jobs WHERE job_id = :jobId")
    fun findJobById(jobId: String): Job?

    @Query("SELECT count(1) FROM jobs")
    suspend fun getJobsCount(): Int
}
