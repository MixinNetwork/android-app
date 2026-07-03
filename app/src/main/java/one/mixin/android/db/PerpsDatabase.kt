package one.mixin.android.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.db.datasource.execSQL
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.db.perps.PerpsOrderDao
import one.mixin.android.db.perps.PerpsPositionDao
import one.mixin.android.util.database.dbDir
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

@Database(
    entities = [
        PerpsPosition::class,
        PerpsOrder::class,
        PerpsMarket::class,
    ],
    version = 5,
)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class PerpsDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: PerpsDatabase? = null
        private val lock = Any()
        private var currentIdentityNumber: String? = null
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override suspend fun migrate(db: SQLiteConnection) {
                    db.execSQL("ALTER TABLE markets ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE markets ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
                }
            }
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override suspend fun migrate(db: SQLiteConnection) {
                    db.execSQL("ALTER TABLE positions ADD COLUMN take_profit_price TEXT")
                    db.execSQL("ALTER TABLE positions ADD COLUMN stop_loss_price TEXT")
                    db.execSQL("ALTER TABLE positions ADD COLUMN liquidation_price TEXT")
                    db.execSQL("ALTER TABLE markets ADD COLUMN price_scale INTEGER NOT NULL DEFAULT 2")
                }
            }
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override suspend fun migrate(db: SQLiteConnection) {
                    db.execSQL("DROP TABLE IF EXISTS position_histories")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS perps_orders (
                            order_id TEXT NOT NULL PRIMARY KEY,
                            position_id TEXT NOT NULL,
                            market_id TEXT NOT NULL,
                            side TEXT NOT NULL,
                            order_type TEXT NOT NULL,
                            status TEXT NOT NULL,
                            leverage INTEGER NOT NULL,
                            quantity TEXT NOT NULL,
                            entry_price TEXT NOT NULL,
                            close_price TEXT NOT NULL,
                            realized_pnl TEXT NOT NULL,
                            roe TEXT NOT NULL,
                            close_reason TEXT,
                            trigger_price TEXT,
                            created_at TEXT NOT NULL,
                            updated_at TEXT NOT NULL
                        )
                        """
                    )
                }
            }
        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override suspend fun migrate(db: SQLiteConnection) {
                    db.execSQL("ALTER TABLE perps_orders ADD COLUMN pay_amount TEXT NOT NULL DEFAULT '0'")
                    db.execSQL("DELETE FROM perps_orders")
                }
            }

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
                    ).setDriver(AndroidSQLiteDriver())
                        .addCallback(
                        object : Callback() {
                            override suspend fun onOpen(db: SQLiteConnection) {
                                super.onOpen(db)
                                db.execSQL("PRAGMA synchronous = NORMAL")
                            }
                        },
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                        .fallbackToDestructiveMigration()
                        .enableMultiInstanceInvalidation()
                        .setQueryCoroutineContext(
                            Executors.newFixedThreadPool(
                                max(2, min(Runtime.getRuntime().availableProcessors() - 1, 4)),
                            ).asCoroutineDispatcher(),
                        )
                    INSTANCE = builder.build()
                    currentIdentityNumber = scopedIdentity
                }
            }
            return INSTANCE as PerpsDatabase
        }
    }

    abstract fun perpsPositionDao(): PerpsPositionDao
    abstract fun perpsOrderDao(): PerpsOrderDao
    abstract fun perpsMarketDao(): PerpsMarketDao

    override fun close() {
        super.close()
        INSTANCE = null
        currentIdentityNumber = null
    }
}
