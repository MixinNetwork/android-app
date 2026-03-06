package one.mixin.android.fts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.session.Session
import one.mixin.android.util.database.dbDir
import java.io.File

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
        private var currentIdentityNumber: String? = null

        private var supportSQLiteDatabase: SupportSQLiteDatabase? = null

        fun destroy(close: Boolean = false) {
            synchronized(lock) {
                if (close) {
                    INSTANCE?.close()
                }
                INSTANCE = null
                currentIdentityNumber = null
                supportSQLiteDatabase = null
            }
        }

        fun getDatabase(
            context: Context,
            identityNumber: String? = null,
        ): FtsDatabase {
            val scopedIdentity = identityNumber?.takeIf { it.isNotBlank() }
                ?: Session.getAccount()?.identityNumber
                ?: "temp"
            synchronized(lock) {
                if (INSTANCE != null && currentIdentityNumber != scopedIdentity) {
                    INSTANCE?.close()
                    INSTANCE = null
                    supportSQLiteDatabase = null
                }
                if (INSTANCE == null) {
                    val dbPath = File(dbDir(context, scopedIdentity), FTS_DB_NAME).absolutePath
                    val builder =
                        Room.databaseBuilder(
                            context,
                            FtsDatabase::class.java,
                            dbPath,
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
                    currentIdentityNumber = scopedIdentity
                }
            }
            return INSTANCE as FtsDatabase
        }
    }

    abstract fun messageMetaDao(): MessageMetaDao

    abstract fun messageFtsDao(): MessageFtsDao

    override fun close() {
        super.close()
        synchronized(lock) {
            if (INSTANCE === this) {
                INSTANCE = null
                currentIdentityNumber = null
                supportSQLiteDatabase = null
            }
        }
    }
}
