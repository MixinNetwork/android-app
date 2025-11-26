package one.mixin.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants
import one.mixin.android.db.converter.AssetChangeListConverter
import one.mixin.android.db.converter.Web3TypeConverters
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3ChainDao
import one.mixin.android.db.web3.Web3RawTransactionDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.util.SINGLE_DB_EXECUTOR
import one.mixin.android.util.database.dbDir
import one.mixin.android.vo.Property
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
        Property::class
    ],
    version = 5,
)
@TypeConverters(Web3TypeConverters::class, AssetChangeListConverter::class)
abstract class WalletDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: WalletDatabase? = null

        private val lock = Any()

        private lateinit var supportSQLiteDatabase: SupportSQLiteDatabase

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS transactions")
                database.execSQL("DELETE FROM properties") // delete old offset
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions` (`transaction_hash` TEXT NOT NULL, `chain_id` TEXT NOT NULL, `address` TEXT NOT NULL, `transaction_type` TEXT NOT NULL, `status` TEXT NOT NULL, `block_number` INTEGER NOT NULL, `fee` TEXT NOT NULL, `senders` TEXT, `receivers` TEXT, `approvals` TEXT, `send_asset_id` TEXT, `receive_asset_id` TEXT, `transaction_at` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`transaction_hash`, `chain_id`, `address`))
                    """
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_address_transaction_at` ON `transactions` (`address`, `transaction_at`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transaction_type_send_asset_id_receive_asset_id_transaction_at` ON `transactions` (`transaction_type`, `send_asset_id`, `receive_asset_id`, `transaction_at`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tokens ADD COLUMN level INTEGER NOT NULL DEFAULT ${Constants.AssetLevel.VERIFIED}")
                database.execSQL("ALTER TABLE transactions ADD COLUMN level INTEGER NOT NULL DEFAULT ${Constants.AssetLevel.UNKNOWN}")
                database.execSQL("DELETE FROM properties WHERE `key` IN (SELECT DISTINCT destination FROM addresses)") // delete old offset
                database.execSQL("DROP INDEX IF EXISTS `index_transactions_transaction_type_send_asset_id_receive_asset_id_transaction_at`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transaction_type_send_asset_id_receive_asset_id_transaction_at_level` ON `transactions` (`transaction_type`, `send_asset_id`, `receive_asset_id`, `transaction_at`, `level`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE addresses ADD COLUMN path TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallets ADD COLUMN owners TEXT")
                database.execSQL("ALTER TABLE wallets ADD COLUMN safe_chain_id TEXT")
                database.execSQL("ALTER TABLE wallets ADD COLUMN safe_address TEXT")
            }
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
                        ).addCallback(
                            object : Callback() {
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onOpen(db)
                                    db.execSQL("PRAGMA synchronous = NORMAL")
                                    supportSQLiteDatabase = db
                                }
                            },
                        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

    override fun close() {
        super.close()
        INSTANCE = null
    }
}