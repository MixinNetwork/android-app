package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.JobShadow

@Dao
interface JobDao : BaseDao<JobShadow> {
    @Query("SELECT * FROM jobs_shadow WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' ORDER BY rowid ASC LIMIT 100")
    suspend fun findAckJobs(): List<JobShadow>

    @Query("SELECT count(1) FROM jobs_shadow WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS'")
    suspend fun findAckJobsCount(): Int

    @Query("SELECT count(1) FROM jobs_shadow WHERE `action` = 'CREATE_MESSAGE'")
    suspend fun findCreateMessageJobsCount(): Int

    @Query("SELECT * FROM jobs_shadow WHERE `action` = 'CREATE_MESSAGE' ORDER BY rowid ASC LIMIT 100")
    suspend fun findCreateMessageJobs(): List<JobShadow>

    @Query("SELECT * FROM jobs_shadow WHERE job_id = :jobId")
    fun findJobById(jobId: String): JobShadow?

    @Query("DELETE FROM jobs WHERE job_id IN (SELECT job_id FROM jobs LIMIT 999)")
    suspend fun clearAckJobs()

    @Query("SELECT count(1) FROM jobs")
    suspend fun getJobsCount(): Int

}
