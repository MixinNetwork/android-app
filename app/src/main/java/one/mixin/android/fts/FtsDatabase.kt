package one.mixin.android.fts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.launch
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinCorruptionCallback
import one.mixin.android.db.MixinOpenHelperFactory
import one.mixin.android.util.database.clearFts
import one.mixin.android.util.reportException
import timber.log.Timber

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
                        FTS_DB_NAME,
                    ).openHelperFactory(
                        MixinOpenHelperFactory(
                            FrameworkSQLiteOpenHelperFactory(),
                            listOf(object : MixinCorruptionCallback {
                                override fun onCorruption(database: SupportSQLiteDatabase) {
                                    MixinApplication.get().applicationScope.launch {
                                        clearFts(MixinApplication.appContext)
                                        Timber.e("Delete fts")
                                    }
                                    val e = IllegalStateException("Fts database is corrupted, current DB version: 1")
                                    reportException(e)
                                }
                            }),
                        ),
                    ).addCallback(
                        object : Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                db.execSQL("PRAGMA synchronous = NORMAL")
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
