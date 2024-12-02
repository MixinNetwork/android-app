package one.mixin.android.crypto.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.crypto.vo.Identity
import one.mixin.android.crypto.vo.PreKey
import one.mixin.android.crypto.vo.RatchetSenderKey
import one.mixin.android.crypto.vo.SenderKey
import one.mixin.android.crypto.vo.Session
import one.mixin.android.crypto.vo.SignedPreKey
import one.mixin.android.db.MixinDatabase

@Database(
    entities = [
        (SenderKey::class),
        (Identity::class),
        (PreKey::class),
        (SignedPreKey::class),
        (Session::class),
        (RatchetSenderKey::class),
    ],
    version = 3,
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun senderKeyDao(): SenderKeyDao

    abstract fun identityDao(): IdentityDao

    abstract fun preKeyDao(): PreKeyDao

    abstract fun signedPreKeyDao(): SignedPreKeyDao

    abstract fun sessionDao(): SessionDao

    abstract fun ratchetSenderKeyDao(): RatchetSenderKeyDao

    companion object {
        private var INSTANCE: SignalDatabase? = null

        private val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP INDEX IF EXISTS index_sessions_address")
                    database.execSQL("ALTER TABLE sessions ADD COLUMN device INTEGER NOT NULL DEFAULT 1")
                    database.execSQL("CREATE UNIQUE INDEX index_sessions_address_device ON sessions (address, device)")
                    database.execSQL("UPDATE sessions SET address = substr(address, 1, 36), device = 1 WHERE length(address) = 38")
                    database.execSQL("ALTER TABLE ratchet_sender_keys ADD COLUMN message_id TEXT")
                    database.execSQL("ALTER TABLE ratchet_sender_keys ADD COLUMN created_at TEXT NOT NULL DEFAULT ''")
                }
            }

        fun getDatabase(context: Context): SignalDatabase {
            if (INSTANCE == null) {
                INSTANCE =
                    Room.databaseBuilder(context, SignalDatabase::class.java, "signal.db")
                        .addMigrations(MIGRATION_2_3)
                        .addCallback(CALLBACK)
                        .build()
            }
            return INSTANCE as SignalDatabase
        }

        private val CALLBACK =
            object : RoomDatabase.Callback() {
            }
    }

    override fun close() {
        INSTANCE = null
        super.close()
    }
}
