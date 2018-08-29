package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Job

@Dao
interface JobDao : BaseDao<Job> {
    @Query("SELECT * FROM jobs WHERE `action` = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' ORDER BY created_at ASC LIMIT 100")
    fun findAckJobs(): LiveData<List<Job>>
}