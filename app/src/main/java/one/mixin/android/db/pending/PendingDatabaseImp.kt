package one.mixin.android.db.pending

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.ArrayMap
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants.DataBase.PENDING_DB_NAME
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.insertNoReplace
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.database.dbDir
import one.mixin.android.util.debug.getContent
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Job
import one.mixin.android.vo.MessageMedia
import java.io.File

@Database(
    entities = [
        (FloodMessage::class),
        (PendingMessage::class),
        (Job::class),
    ],
    version = 1,
)
abstract class PendingDatabaseImp : RoomDatabase(), PendingDatabase {
    abstract override fun floodMessageDao(): FloodMessageDao

    abstract override fun pendingMessageDao(): PendingMessageDao

    abstract override fun jobDao(): JobDao

    companion object {
        private var INSTANCE: PendingDatabaseImp? = null

        private val lock = Any()

        fun getDatabase(
            context: Context,
            floodMessageDao: FloodMessageDao,
            jobDao: JobDao,
        ): PendingDatabaseImp {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val dir = dbDir(context)
                    val builder =
                        Room.databaseBuilder(
                            context,
                            PendingDatabaseImp::class.java,
                            File(dir, PENDING_DB_NAME).absolutePath
                        ).enableMultiInstanceInvalidation().addCallback(
                            object : Callback() {
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onOpen(db)
                                    supportSQLiteDatabase = db
                                }

                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    while (true) {
                                        val list = floodMessageDao.limit100()
                                        list.forEach { msg ->
                                            val values = ContentValues()
                                            values.put("message_id", msg.messageId)
                                            values.put("data", msg.data)
                                            values.put("created_at", msg.createdAt)
                                            db.insert("flood_messages", SQLiteDatabase.CONFLICT_REPLACE, values)
                                        }
                                        floodMessageDao.deleteList(list)
                                        if (list.size < 100) {
                                            break
                                        }
                                    }
                                    while (true) {
                                        val list = jobDao.limit100()
                                        list.forEach { job ->
                                            val values = ContentValues()
                                            values.put("job_id", job.jobId)
                                            values.put("action", job.action)
                                            values.put("created_at", job.createdAt)
                                            values.put("order_id", job.orderId)
                                            values.put("user_id", job.userId)
                                            values.put("priority", job.priority)
                                            values.put("blaze_message", job.blazeMessage)
                                            values.put("conversation_id", job.conversationId)
                                            values.put("resend_message_id", job.resendMessageId)
                                            values.put("run_count", job.runCount)
                                            db.insert("jobs", SQLiteDatabase.CONFLICT_REPLACE, values)
                                        }
                                        jobDao.deleteList(list)
                                        if (list.size < 100) {
                                            break
                                        }
                                    }
                                }
                            },
                        )
                    INSTANCE = builder.build()
                }
            }
            return INSTANCE as PendingDatabaseImp
        }

        private var supportSQLiteDatabase: SupportSQLiteDatabase? = null

        fun query(query: String): String? {
            val start = System.currentTimeMillis()
            var cursor: Cursor? = null
            try {
                cursor =
                    supportSQLiteDatabase?.query(query) ?: return null
                cursor.moveToFirst()
                val result = ArrayList<ArrayMap<String, String>>()
                do {
                    val map = ArrayMap<String, String>()
                    for (i in 0 until cursor.columnCount) {
                        map[cursor.getColumnName(i)] = cursor.getContent(i)
                    }
                    result.add(map)
                } while (cursor.moveToNext())
                return "${GsonHelper.customGson.toJson(result)} ${System.currentTimeMillis() - start}ms"
            } catch (e: Exception) {
                return e.message
            } finally {
                cursor?.close()
            }
        }
    }

    override suspend fun getPendingMessages() = pendingMessageDao().getMessages()

    override fun deletePendingMessageByIds(ids: List<String>) {
        pendingMessageDao().deleteByIds(ids)
    }

    override fun getLastBlazeMessageCreatedAt(): String? = floodMessageDao().getLastBlazeMessageCreatedAt()

    override fun insertJob(job: Job) = jobDao().insertNoReplace(job)

    override suspend fun findFloodMessages() = floodMessageDao().findFloodMessages()

    override suspend fun findMessageIdsLimit10() = floodMessageDao().findMessageIdsLimit10()

    override suspend fun findMaxLengthMessageId(ids: List<String>) = floodMessageDao().findMaxLengthMessageId(ids)

    override suspend fun deleteFloodMessageById(id: String) = floodMessageDao().deleteFloodMessageById(id)

    override suspend fun deleteEmptyMessages() = floodMessageDao().deleteEmptyMessages()

    override fun insertFloodMessage(floodMessage: FloodMessage) = floodMessageDao().insert(floodMessage)

    override fun deleteFloodMessage(floodMessage: FloodMessage) = floodMessageDao().delete(floodMessage)

    override fun addObserver(observer: InvalidationTracker.Observer) {
        invalidationTracker.addObserver(observer)
    }

    override fun removeObserver(observer: InvalidationTracker.Observer) {
        invalidationTracker.removeObserver(observer)
    }

    override fun insertJobs(jobs: List<Job>) {
        jobDao().insertList(jobs)
    }

    override fun deletePendingMessageById(messageId: String) {
        pendingMessageDao().deleteById(messageId)
    }

    override fun findMessageMediaById(messageId: String): MessageMedia? {
        return pendingMessageDao().findMessageMediaById(messageId)
    }
}
