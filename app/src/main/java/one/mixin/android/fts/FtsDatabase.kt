package one.mixin.android.fts

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.datasource.execSQL
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

        fun destroy(close: Boolean = false) {
            synchronized(lock) {
                if (close) {
                    INSTANCE?.close()
                }
                INSTANCE = null
                currentIdentityNumber = null
            }
        }

        fun getDatabase(
            context: Context,
            identityNumber: String,
        ): FtsDatabase {
            val scopedIdentity = identityNumber.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("identityNumber is required for FtsDatabase")
            synchronized(lock) {
                if (INSTANCE != null && currentIdentityNumber != scopedIdentity) {
                    INSTANCE?.close()
                    INSTANCE = null
                }
                if (INSTANCE == null) {
                    val dbPath = File(dbDir(context, scopedIdentity), FTS_DB_NAME).absolutePath
                    val builder =
                        Room.databaseBuilder(
                            context,
                            FtsDatabase::class.java,
                            dbPath,
                        ).setDriver(AndroidSQLiteDriver())
                            .addCallback(
                            object : Callback() {
                                override suspend fun onOpen(db: SQLiteConnection) {
                                    super.onOpen(db)
                                    db.execSQL("PRAGMA synchronous = NORMAL")
                                }
                            },
                        )
                    val database = builder.build()
                    INSTANCE = database
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
            }
        }
    }
}
