package one.mixin.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.api.response.Web3Token
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.fts.MessageMetaDao
import one.mixin.android.session.Session
import one.mixin.android.util.database.dbDir
import java.io.File

@Database(
    entities = [
        Web3Token::class,
    ],
    version = 1,
)
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
                            File(dir, Constants.DataBase.WALLET_DB_NAME).absolutePath,
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

    override fun close() {
        super.close()
        INSTANCE = null
    }
}