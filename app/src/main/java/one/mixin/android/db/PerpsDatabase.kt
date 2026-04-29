package one.mixin.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionHistory
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.db.perps.PerpsPositionHistoryDao
import one.mixin.android.util.SINGLE_DB_EXECUTOR
import one.mixin.android.util.database.dbDir
import one.mixin.android.util.reportException
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

@Database(
    entities = [
        PerpsPosition::class,
        PerpsPositionHistory::class,
        PerpsMarket::class,
    ],
    version = 1,
)
abstract class PerpsDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: PerpsDatabase? = null
        private val lock = Any()
        private var currentIdentityNumber: String? = null

        fun getDatabase(
            context: Context,
            identityNumber: String,
        ): PerpsDatabase {
            val scopedIdentity = identityNumber.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("identityNumber is required for PerpsDatabase")
            synchronized(lock) {
                if (INSTANCE != null && currentIdentityNumber != scopedIdentity) {
                    INSTANCE?.close()
                    INSTANCE = null
                }
                if (INSTANCE == null) {
                    val dir = dbDir(context, scopedIdentity)
                    val builder = Room.databaseBuilder(
                        context,
                        PerpsDatabase::class.java,
                        File(dir, Constants.DataBase.PERPS_DB_NAME).absolutePath,
                    ).openHelperFactory(
                        MixinOpenHelperFactory(
                            FrameworkSQLiteOpenHelperFactory(),
                            listOf(
                                object : MixinCorruptionCallback {
                                    override fun onCorruption(database: SupportSQLiteDatabase) {
                                        val e = IllegalStateException("Perps database is corrupted, current DB version: 1")
                                        reportException(e)
                                    }
                                },
                            ),
                        ),
                    ).addCallback(
                        object : Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                db.execSQL("PRAGMA synchronous = NORMAL")
                            }
                        },
                    ).fallbackToDestructiveMigration(dropAllTables = true)
                        .enableMultiInstanceInvalidation()
                        .setQueryExecutor(
                            Executors.newFixedThreadPool(
                                max(2, min(Runtime.getRuntime().availableProcessors() - 1, 4)),
                            ),
                        )
                        .setTransactionExecutor(SINGLE_DB_EXECUTOR)
                    INSTANCE = builder.build()
                    currentIdentityNumber = scopedIdentity
                }
            }
            return INSTANCE as PerpsDatabase
        }
    }

    abstract fun perpsPositionDao(): PerpsPositionDao
    abstract fun perpsPositionHistoryDao(): PerpsPositionHistoryDao
    abstract fun perpsMarketDao(): PerpsMarketDao

    override fun close() {
        super.close()
        INSTANCE = null
        currentIdentityNumber = null
    }
}
