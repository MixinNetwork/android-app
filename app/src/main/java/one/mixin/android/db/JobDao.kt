package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Job

@Dao
interface JobDao : BaseDao<Job> {
    @Query("SELECT * FROM jobs WHERE job_action = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' ORDER BY created_at ASC LIMIT 100")
    fun findAckJobs(): LiveData<List<Job>>

    @Query("DELETE FROM jobs WHERE job_id = :id")
    fun deleteById(id: String)
}