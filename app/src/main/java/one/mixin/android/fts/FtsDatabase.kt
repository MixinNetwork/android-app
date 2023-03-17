package one.mixin.android.fts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MessageFts::class,
        MessagesMeta::class,
    ],
    version = 1,
)
abstract class FtsDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: FtsDatabase? = null

        private val lock = Any()

        private lateinit var supportSQLiteDatabase: SupportSQLiteDatabase
        fun getDatabase(context: Context): FtsDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder = Room.databaseBuilder(
                        context,
                        FtsDatabase::class.java,
                        "fts.db",
                    ).addCallback(
                        object : Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                supportSQLiteDatabase = db
                            }
                        },
                    )
                    INSTANCE = builder.build()
                }
            }
            return INSTANCE as FtsDatabase
        }
    }

    abstract fun messageMetaDao(): MessageMetaDao

    abstract fun messageFtsDao(): MessageFtsDao
}
