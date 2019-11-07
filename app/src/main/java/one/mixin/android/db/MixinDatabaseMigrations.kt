package one.mixin.android.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants.DataBase.MINI_VERSION

class MixinDatabaseMigrations private constructor() {

    companion object {
        val MIGRATION_15_16: Migration = object : Migration(MINI_VERSION, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) ")
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL("CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                    "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)")
            }
        }

        val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                    "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                    "(job_id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                    "status, created_at)")
            }
        }

        val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
            }
        }

        val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
            }
        }

        val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL("CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                    "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                    "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ")
            }
        }

        val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE assets ADD COLUMN asset_key TEXT")
            }
        }

        val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN thumb_url TEXT")
            }
        }

        val MIGRATION_22_23: Migration = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN biography TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_23_24: Migration = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS addresses (address_id TEXT NOT NULL, type TEXT NOT NULL, 
                    asset_id TEXT NOT NULL, destination TEXT NOT NULL, label TEXT NOT NULL, updated_at TEXT NOT NULL, 
                    reserve TEXT NOT NULL, fee TEXT NOT NULL, tag TEXT, dust TEXT, PRIMARY KEY(address_id))
                    """
                )
                database.execSQL("CREATE TABLE IF NOT EXISTS assets_extra (asset_id TEXT NOT NULL, hidden INTEGER, PRIMARY KEY(asset_id))")
                database.execSQL("INSERT OR REPLACE INTO assets_extra (asset_id, hidden) SELECT asset_id, hidden FROM assets WHERE hidden = 1")

                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assets (asset_id TEXT NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, 
                    icon_url TEXT NOT NULL, balance TEXT NOT NULL, destination TEXT NOT NULL, tag TEXT, price_btc TEXT NOT NULL, 
                    price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, 
                    confirmations INTEGER NOT NULL, asset_key TEXT, PRIMARY KEY(asset_id))
                    """
                )
                database.execSQL("DROP TABLE IF EXISTS top_assets")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS top_assets (asset_id TEXT NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, 
                    icon_url TEXT NOT NULL, balance TEXT NOT NULL, destination TEXT NOT NULL, tag TEXT, price_btc TEXT NOT NULL, 
                    price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, 
                    confirmations INTEGER NOT NULL, PRIMARY KEY(asset_id))
                    """
                )
            }
        }

        val MIGRATION_24_25: Migration = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS participant_session (`conversation_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `sent_to_server` INTEGER, `created_at` TEXT, PRIMARY KEY(`conversation_id`, `user_id`, `session_id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS session_sync (`conversation_id` TEXT NOT NULL, `created_at` TEXT, PRIMARY KEY(`conversation_id`))")
            }
        }
    }
}
