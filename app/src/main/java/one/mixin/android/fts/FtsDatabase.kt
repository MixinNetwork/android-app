package one.mixin.android.fts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.Constants.DataBase.FTS_DB_VERSION
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinCorruptionCallback
import one.mixin.android.db.MixinOpenHelperFactory
import one.mixin.android.ui.repair.RepairActivity
import one.mixin.android.util.reportException
import timber.log.Timber

@Database(
    entities = [
        MessageFts::class,
        MessagesMeta::class,
    ],
    version = FTS_DB_VERSION,
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
                                    val e = IllegalStateException("Fts database is corrupted, current DB version: $FTS_DB_VERSION")
                                    Timber.e(e.message)
                                    reportException(e)
                                    MixinApplication.get().gotoRepair(RepairActivity.DbType.FTS)
                                }
                            }),
                        ),
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
