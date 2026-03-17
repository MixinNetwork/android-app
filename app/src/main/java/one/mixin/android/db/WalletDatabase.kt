package one.mixin.android.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_LOGIN_OR_SIGN_UP
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.converter.AssetChangeListConverter
import one.mixin.android.db.converter.Web3TypeConverters
import one.mixin.android.db.web3.SafeWalletsDao
import one.mixin.android.db.web3.WalletOutputDao
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3ChainDao
import one.mixin.android.db.web3.Web3RawTransactionDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.SafeWallets
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.moveTo
import one.mixin.android.util.SINGLE_DB_EXECUTOR
import one.mixin.android.util.database.dbDir
import one.mixin.android.util.reportException
import one.mixin.android.vo.Property
import one.mixin.android.vo.route.Order
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

@Database(
    entities = [
        Web3Token::class,
        Web3Transaction::class,
        Web3Wallet::class,
        Web3Address::class,
        Web3TokensExtra::class,
        Web3Chain::class,
        Web3RawTransaction::class,
        Property::class,
        Order::class,
        SafeWallets::class,
        WalletOutput::class,
    ],
    version = 7,
)
@TypeConverters(Web3TypeConverters::class, AssetChangeListConverter::class)
abstract class WalletDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: WalletDatabase? = null

        private val lock = Any()

        private lateinit var supportSQLiteDatabase: SupportSQLiteDatabase

        private const val ACCOUNT_PROPERTY_KEY: String = "account"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS transactions")
                db.execSQL("DELETE FROM properties") // delete old offset
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions` (`transaction_hash` TEXT NOT NULL, `chain_id` TEXT NOT NULL, `address` TEXT NOT NULL, `transaction_type` TEXT NOT NULL, `status` TEXT NOT NULL, `block_number` INTEGER NOT NULL, `fee` TEXT NOT NULL, `senders` TEXT, `receivers` TEXT, `approvals` TEXT, `send_asset_id` TEXT, `receive_asset_id` TEXT, `transaction_at` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`transaction_hash`, `chain_id`, `address`))
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_address_transaction_at` ON `transactions` (`address`, `transaction_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transaction_type_send_asset_id_receive_asset_id_transaction_at` ON `transactions` (`transaction_type`, `send_asset_id`, `receive_asset_id`, `transaction_at`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tokens ADD COLUMN level INTEGER NOT NULL DEFAULT ${Constants.AssetLevel.VERIFIED}")
                db.execSQL("ALTER TABLE transactions ADD COLUMN level INTEGER NOT NULL DEFAULT ${Constants.AssetLevel.UNKNOWN}")
                db.execSQL("DELETE FROM properties WHERE `key` IN (SELECT DISTINCT destination FROM addresses)") // delete old offset
                db.execSQL("DROP INDEX IF EXISTS `index_transactions_transaction_type_send_asset_id_receive_asset_id_transaction_at`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transaction_type_send_asset_id_receive_asset_id_transaction_at_level` ON `transactions` (`transaction_type`, `send_asset_id`, `receive_asset_id`, `transaction_at`, `level`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE addresses ADD COLUMN path TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`order_id` TEXT NOT NULL, `wallet_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `pay_asset_id` TEXT NOT NULL, `receive_asset_id` TEXT NOT NULL, `pay_amount` TEXT NOT NULL, `receive_amount` TEXT, `pay_trace_id` TEXT, `receive_trace_id` TEXT, `state` TEXT NOT NULL, `created_at` TEXT NOT NULL, `order_type` TEXT NOT NULL, `fund_status` TEXT, `price` TEXT, `pending_amount` TEXT, `filled_receive_amount` TEXT, `expected_receive_amount` TEXT, `expired_at` TEXT, PRIMARY KEY(`order_id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_state_created_at` ON `orders` (`state`, `created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_order_type_created_at` ON `orders` (`order_type`, `created_at`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `safe_wallets` (`wallet_id` TEXT NOT NULL, `name` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, `role` TEXT NOT NULL, `chain_id` TEXT NOT NULL, `address` TEXT NOT NULL, `url` TEXT NOT NULL, PRIMARY KEY(`wallet_id`))")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `outputs` (`output_id` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `transaction_hash` TEXT NOT NULL, `output_index` INTEGER NOT NULL, `amount` TEXT NOT NULL, `address` TEXT NOT NULL, `pubkey_hex` TEXT NOT NULL, `pubkey_type` TEXT NOT NULL, `status` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`output_id`))")
            }
        }

        fun moveTempDatabaseFileIfNeeded(context: Context, identityNumber: String?) {
            val shouldGoWallet: Boolean = context.defaultSharedPreferences.getBoolean(PREF_LOGIN_OR_SIGN_UP, false)
            if (shouldGoWallet) { // Do not migrate on first entry
                return
            }
            if (identityNumber.isNullOrBlank()) {
                return
            }
            val targetDir: File = dbDir(context, identityNumber)
            val targetDbFile = File(targetDir, Constants.DataBase.WEB3_DB_NAME)
            if (targetDbFile.exists() && targetDbFile.length() > 0) {
                return
            }
            val tempDir: File = dbDir(context, "temp")
            val tempDbFile = File(tempDir, Constants.DataBase.WEB3_DB_NAME)
            if (!tempDbFile.exists() || tempDbFile.length() <= 0) {
                return
            }
            val storedIdentityNumber: String? = readIdentityNumberFromDatabaseFile(tempDbFile)
            if (storedIdentityNumber != identityNumber) {
                tempDbFile.delete()
                File(tempDir, Constants.DataBase.WEB3_DB_NAME + "-wal").delete()
                File(tempDir, Constants.DataBase.WEB3_DB_NAME + "-shm").delete()
                File(tempDir, Constants.DataBase.WEB3_DB_NAME + "-journal").delete()
                return
            }
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            tempDbFile.moveTo(targetDbFile)
            moveSidecarIfExists(tempDir, targetDir, Constants.DataBase.WEB3_DB_NAME, "-wal")
            moveSidecarIfExists(tempDir, targetDir, Constants.DataBase.WEB3_DB_NAME, "-shm")
            moveSidecarIfExists(tempDir, targetDir, Constants.DataBase.WEB3_DB_NAME, "-journal")
        }

        private fun readIdentityNumberFromDatabaseFile(databaseFile: File): String? {
            var cursor: Cursor? = null
            var database: SQLiteDatabase? = null
            return try {
                database = SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                cursor = database.rawQuery("SELECT value FROM properties WHERE `key` = ? LIMIT 1", arrayOf(ACCOUNT_PROPERTY_KEY))
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            } finally {
                cursor?.close()
                database?.close()
            }
        }

        private fun moveSidecarIfExists(
            fromDir: File,
            toDir: File,
            databaseName: String,
            suffix: String,
        ) {
            val fromFile = File(fromDir, databaseName + suffix)
            if (!fromFile.exists()) {
                return
            }
            val toFile = File(toDir, databaseName + suffix)
            fromFile.moveTo(toFile)
        }

        fun getDatabase(context: Context): WalletDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val dir = dbDir(context)
                    val builder =
                        Room.databaseBuilder(
                            context,
                            WalletDatabase::class.java,
                            File(dir, Constants.DataBase.WEB3_DB_NAME).absolutePath,
                        ).openHelperFactory(
                            MixinOpenHelperFactory(
                                FrameworkSQLiteOpenHelperFactory(),
                                listOf(
                                    object : MixinCorruptionCallback {
                                        override fun onCorruption(database: SupportSQLiteDatabase) {
                                            val e = IllegalStateException("Wallet database is corrupted, current DB version: 7")
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
                                    supportSQLiteDatabase = db
                                }
                            },
                        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .enableMultiInstanceInvalidation()
                            .setQueryExecutor(
                                Executors.newFixedThreadPool(
                                    max(
                                        2,
                                        min(Runtime.getRuntime().availableProcessors() - 1, 4),
                                    ),
                                ),
                            )
                            .setTransactionExecutor(SINGLE_DB_EXECUTOR)
                    INSTANCE = builder.build()
                }
            }
            return INSTANCE as WalletDatabase
        }
    }

    abstract fun web3TokenDao(): Web3TokenDao
    abstract fun web3TransactionDao(): Web3TransactionDao
    abstract fun web3WalletDao(): Web3WalletDao
    abstract fun web3AddressDao(): Web3AddressDao
    abstract fun web3TokensExtraDao(): Web3TokensExtraDao
    abstract fun web3ChainDao(): Web3ChainDao
    abstract fun web3PropertyDao(): Web3PropertyDao
    abstract fun web3RawTransactionDao(): Web3RawTransactionDao

    abstract fun walletOutputDao(): WalletOutputDao
    abstract fun orderDao(): OrderDao
    abstract fun safeWalletsDao(): SafeWalletsDao

    override fun close() {
        super.close()
        INSTANCE = null
    }
}