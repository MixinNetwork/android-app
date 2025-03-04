package one.mixin.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants
import one.mixin.android.api.response.Web3Address
import one.mixin.android.api.response.Web3Wallet
import one.mixin.android.db.converter.Web3TypeConverters
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3ChainDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.util.database.dbDir
import one.mixin.android.vo.Chain
import java.io.File

@Database(
    entities = [
        Web3Token::class,
        Web3Transaction::class,
        Web3Wallet::class,
        Web3Address::class,
        Web3TokensExtra::class,
        Web3Chain::class,
    ],
    version = 1,
)
@TypeConverters(Web3TypeConverters::class)
abstract class WalletDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: WalletDatabase? = null

        private val lock = Any()

        private lateinit var supportSQLiteDatabase: SupportSQLiteDatabase

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
                        )
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

    override fun close() {
        super.close()
        INSTANCE = null
    }
}