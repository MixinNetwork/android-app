package one.mixin.android.db.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Job

@Database(
    entities = [
        (FloodMessage::class),
        (CacheMessage::class),
        (Job::class)
    ],
    version = 1
)
abstract class CacheDataBase : RoomDatabase() {
    abstract fun floodMessageDao(): FloodMessageDao
    abstract fun cacheMessageDao(): CacheMessageDao

    abstract fun jobDao(): JobDao

    companion object {
        private var INSTANCE: CacheDataBase? = null

        private val lock = Any()

        fun getDatabase(context: Context, floodMessageDao: FloodMessageDao, jobDao: JobDao): CacheDataBase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder = Room.databaseBuilder(
                        context, CacheDataBase::class.java,
                        "cache.db"
                    ).enableMultiInstanceInvalidation().addCallback(
                        object : Callback() {
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
                        }
                    )
                    INSTANCE = builder.build()
                }
            }
            return INSTANCE as CacheDataBase
        }
    }
}
