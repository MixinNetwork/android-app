package one.mixin.android.db.pending

import androidx.room.InvalidationTracker
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageMedia

interface PendingDatabase {

    fun getLastBlazeMessageCreatedAt(): String?

    fun insertJob(job: Job)

    suspend fun findFloodMessages(): List<FloodMessage>

    fun insertFloodMessage(floodMessage: FloodMessage)

    fun addObserver(observer: InvalidationTracker.Observer)

    fun removeObserver(observer: InvalidationTracker.Observer)

    fun deleteFloodMessage(floodMessage: FloodMessage)

    suspend fun getPendingMessages(): List<Message>

    fun deletePendingMessageByIds(ids: List<String>)

    fun findMessageMediaById(messageId: String): MessageMedia?

    fun deletePendingMessageById(messageId: String)

    fun insertJobs(jobs: List<Job>)

    fun jobDao(): JobDao

    fun floodMessageDao(): FloodMessageDao

    fun pendingMessageDao(): PendingMessageDao
}
