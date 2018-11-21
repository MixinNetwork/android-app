package one.mixin.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.vo.Address
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Hyperlink
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.Offset
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ResendMessage
import one.mixin.android.vo.SentSenderKey
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.User

@Database(entities = [
    (User::class),
    (Conversation::class),
    (Message::class),
    (Participant::class),
    (Offset::class),
    (Asset::class),
    (Snapshot::class),
    (MessageHistory::class),
    (SentSenderKey::class),
    (Sticker::class),
    (StickerAlbum::class),
    (App::class),
    (Hyperlink::class),
    (FloodMessage::class),
    (Address::class),
    (ResendMessage::class),
    (StickerRelationship::class),
    (TopAsset::class),
    (Job::class)], version = 20)
abstract class MixinDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun participantDao(): ParticipantDao
    abstract fun offsetDao(): OffsetDao
    abstract fun assetDao(): AssetDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun messageHistoryDao(): MessageHistoryDao
    abstract fun sentSenderKeyDao(): SentSenderKeyDao
    abstract fun stickerDao(): StickerDao
    abstract fun stickerAlbumDao(): StickerAlbumDao
    abstract fun appDao(): AppDao
    abstract fun hyperlinkDao(): HyperlinkDao
    abstract fun floodMessageDao(): FloodMessageDao
    abstract fun jobDao(): JobDao
    abstract fun addressDao(): AddressDao
    abstract fun resendMessageDao(): ResendMessageDao
    abstract fun stickerRelationshipDao(): StickerRelationshipDao
    abstract fun topAssetDao(): TopAssetDao

    companion object {
        private var INSTANCE: MixinDatabase? = null
        private var READINSTANCE: MixinDatabase? = null

        private val lock = Any()
        private val readlock = Any()
        private var supportSQLiteDatabase: SupportSQLiteDatabase? = null

        private val MIGRATION_15_17: Migration = object : Migration(15, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) ")
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL("CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                    "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
            }
        }

        private val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
            }
        }

        private val MIGRATION_15_18: Migration = object : Migration(15, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) ")
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL("CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                    "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
            }
        }

        private val MIGRATION_16_18: Migration = object : Migration(16, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
            }
        }

        private val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
            }
        }

        private val MIGRATION_15_19: Migration = object : Migration(15, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) ")
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL("CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                    "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
            }
        }

        private val MIGRATION_16_19: Migration = object : Migration(16, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
            }
        }

        private val MIGRATION_17_19: Migration = object : Migration(17, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
            }
        }

        private val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
            }
        }

        private val MIGRATION_15_20: Migration = object : Migration(15, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) ")
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL("CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                    "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ")
            }
        }

        private val MIGRATION_16_20: Migration = object : Migration(16, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ")
            }
        }

        private val MIGRATION_17_20: Migration = object : Migration(17, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ")
            }
        }

        private val MIGRATION_18_20: Migration = object : Migration(18, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ")
            }
        }

        private val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ")
            }
        }

        fun getDatabase(context: Context): MixinDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context, MixinDatabase::class.java, "mixin.db")
                        .addMigrations(MIGRATION_15_17, MIGRATION_16_17)
                        .addMigrations(MIGRATION_15_18, MIGRATION_16_18, MIGRATION_17_18)
                        .addMigrations(MIGRATION_15_19, MIGRATION_16_19, MIGRATION_17_19, MIGRATION_18_19)
                        .addMigrations(MIGRATION_15_20, MIGRATION_16_20, MIGRATION_17_20, MIGRATION_18_20, MIGRATION_19_20)
                        .enableMultiInstanceInvalidation()
                        .addCallback(CALLBACK)
                        .build()
                }
                return INSTANCE as MixinDatabase
            }
        }

        fun checkPoint() {
            supportSQLiteDatabase?.query("PRAGMA wal_checkpoint(FULL)")?.close()
        }

        fun getReadDatabase(context: Context): MixinDatabase {
            synchronized(readlock) {
                if (READINSTANCE == null) {
                    READINSTANCE = Room.databaseBuilder(context, MixinDatabase::class.java, "mixin.db")
                        .addMigrations(MIGRATION_15_17, MIGRATION_16_17)
                        .addMigrations(MIGRATION_15_18, MIGRATION_16_18, MIGRATION_17_18)
                        .addMigrations(MIGRATION_15_19, MIGRATION_16_19, MIGRATION_17_19, MIGRATION_18_19)
                        .addMigrations(MIGRATION_15_20, MIGRATION_16_20, MIGRATION_17_20, MIGRATION_18_20, MIGRATION_19_20)
                        .enableMultiInstanceInvalidation()
                        .build()
                }
                return READINSTANCE as MixinDatabase
            }
        }

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("CREATE TRIGGER conversation_last_message_update AFTER INSERT ON messages BEGIN UPDATE conversations SET last_message_id = new.id WHERE conversation_id = new.conversation_id; END")
                db.execSQL("CREATE TRIGGER conversation_last_message_delete AFTER DELETE ON messages BEGIN UPDATE conversations SET last_message_id = (select id from messages where conversation_id = old.conversation_id order by created_at DESC limit 1) WHERE conversation_id = old.conversation_id; END")
                db.execSQL("CREATE TRIGGER conversation_unseen_message_count_insert AFTER INSERT ON messages BEGIN UPDATE conversations SET unseen_message_count = (SELECT count(m.id) FROM messages m, users u WHERE m.user_id = u.user_id AND u.relationship != 'ME' AND m.status = 'DELIVERED' AND conversation_id = new.conversation_id) where conversation_id = new.conversation_id; END")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                supportSQLiteDatabase = db
                db.execSQL("CREATE TRIGGER IF NOT EXISTS conversation_last_message_update AFTER INSERT ON messages BEGIN UPDATE conversations SET " +
                    "last_message_id = new.id WHERE conversation_id = new.conversation_id; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS conversation_last_message_delete AFTER DELETE ON messages BEGIN UPDATE conversations SET last_message_id = (select id from messages where conversation_id = old.conversation_id order by created_at DESC limit 1) WHERE conversation_id = old.conversation_id; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS conversation_unseen_message_count_insert AFTER INSERT ON messages BEGIN UPDATE conversations SET unseen_message_count = (SELECT count(m.id) FROM messages m, users u WHERE m.user_id = u.user_id AND u.relationship != 'ME' AND m.status = 'DELIVERED' AND conversation_id = new.conversation_id) where conversation_id = new.conversation_id; END")
            }
        }
    }
}
