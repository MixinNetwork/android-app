package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Job
import one.mixin.android.websocket.BlazeAckMessage

@Dao
interface JobDao : BaseDao<Job> {
    @Query("SELECT job_id as message_id, status as status FROM jobs WHERE job_action = 'ACKNOWLEDGE_MESSAGE_RECEIPTS' LIMIT 100")
    fun findAckJobs(): LiveData<List<BlazeAckMessage>>

    @Query("DELETE FROM jobs WHERE job_id = :id")
    fun deleteById(id: String)
}