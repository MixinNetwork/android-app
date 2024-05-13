package one.mixin.android.db.web3

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import one.mixin.android.Constants.DataBase.WEB3_DB_NAME
import one.mixin.android.api.response.Web3Account
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.db.converter.AppMetadataConverter
import one.mixin.android.db.converter.ApprovalConverter
import one.mixin.android.db.converter.Web3FeeConverter
import one.mixin.android.db.converter.Web3TransferListConverter

@Database(
    entities = [
        Web3Account::class,
        Web3Transaction::class,
    ],
    version = 1,
)
@TypeConverters(Web3FeeConverter::class, Web3TransferListConverter::class, ApprovalConverter::class, AppMetadataConverter::class)
abstract class Web3Database : RoomDatabase() {
    abstract fun web3AccountDao(): Web3AccountDao
    abstract fun web3TransactionDao(): Web3TransactionDao

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