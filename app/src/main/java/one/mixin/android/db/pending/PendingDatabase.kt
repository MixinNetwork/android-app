package one.mixin.android.db.pending

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job as CoroutineJob
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

    suspend fun findMessageIdsLimit10(): List<String>

    suspend fun findMaxLengthMessageId(ids: List<String>): String?

    suspend fun deleteFloodMessageById(id: String)

    suspend fun deleteEmptyMessages()

    fun insertFloodMessage(floodMessage: FloodMessage)

    fun observeInvalidation(
        scope: CoroutineScope,
        tableName: String,
        onInvalidated: () -> Unit,
    ): CoroutineJob

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
