package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Job

@Dao
interface JobDao : BaseDao<Job> {
    @Query("SELECT * FROM jobs WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' ORDER BY rowid ASC LIMIT 100")
    suspend fun findAckJobs(): List<Job>

    @Query("SELECT count(1) FROM jobs WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS'")
    suspend fun findAckJobsCount(): Int

    @Query("SELECT * FROM jobs WHERE `action` = 'CREATE_MESSAGE' ORDER BY created_at ASC LIMIT 100")
    suspend fun findCreateMessageJobs(): List<Job>

    @Query("SELECT * FROM jobs WHERE job_id = :jobId")
    fun findJobById(jobId: String): Job?

    @Query("SELECT count(1) FROM jobs")
    suspend fun getJobsCount(): Int

    @Query("SELECT job_id FROM jobs WHERE job_id IN (:ids)")
    fun findJobsByIds(ids: List<String>): List<String>
}
