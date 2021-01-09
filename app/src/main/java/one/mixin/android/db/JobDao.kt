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

    @Query("SELECT count(1) FROM jobs WHERE `action` = 'CREATE_MESSAGE'")
    suspend fun findCreateMessageJobsCount(): Int

    @Query("SELECT * FROM jobs WHERE `action` = 'CREATE_MESSAGE' ORDER BY rowid ASC LIMIT 100")
    suspend fun findCreateMessageJobs(): List<Job>

    @Query("DELETE FROM jobs WHERE job_id IN (SELECT job_id FROM jobs LIMIT 10000)")
    suspend fun clearAckJobs()

    @Query("SELECT count(1) FROM jobs")
    suspend fun getJobsCount(): Int

    @Query("SELECT * FROM jobs WHERE job_id = :jobId")
    fun findJobById(jobId: String): Job?
}
