package one.mixin.android.db.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.vo.FloodMessage

@Database(
    entities = [
        (FloodMessage::class),
        (Message::class)
    ],
    version = 1
)
abstract class CacheDataBase : RoomDatabase() {
    abstract fun floodMessageDao(): FloodMessageDao

    companion object {
        private var INSTANCE: CacheDataBase? = null

        private val lock = Any()

        fun getDatabase(context: Context, floodMessageDao: FloodMessageDao): CacheDataBase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder = Room.databaseBuilder(
                        context, CacheDataBase::class.java,
                        "Cache.db"
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
