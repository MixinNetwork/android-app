package one.mixin.android.db.web3

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import one.mixin.android.Constants.DataBase.WEB3_DB_NAME
import one.mixin.android.vo.web3.Transaction

@Database(
    entities = [
        Transaction::class
    ],
    version = 1,
)
abstract class Web3Database : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        private var INSTANCE: Web3Database? = null

        private val lock = Any()

        fun getDatabase(
            context: Context,
        ): Web3Database {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder =
                        Room.databaseBuilder(
                            context,
                            Web3Database::class.java,
                            WEB3_DB_NAME,
                        ).enableMultiInstanceInvalidation()
                    INSTANCE = builder.build()
                }
            }
            return INSTANCE as Web3Database
        }
    }
}