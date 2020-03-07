package one.mixin.android.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants.DataBase.MINI_VERSION

class MixinDatabaseMigrations private constructor() {

    companion object {
        val MIGRATION_15_16: Migration = object : Migration(MINI_VERSION, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS assets")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                        "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                        "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) "
                )
                database.execSQL("DROP TABLE IF EXISTS addresses")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                        "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)"
                )
            }
        }

        val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                        "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                        "(job_id))"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                        "status, created_at)"
                )
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
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                        "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                        "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) "
                )
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
                database.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_insert")
            }
        }

        val MIGRATION_25_26: Migration = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS favorite_apps (`app_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`app_id`, `user_id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS resend_session_messages (`message_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `status` INTEGER NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`message_id`, `user_id`, `session_id`))")
            }
        }

        val MIGRATION_26_27: Migration = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `new_snapshots` (`snapshot_id` TEXT NOT NULL, `type` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `amount` TEXT NOT NULL, `created_at` TEXT NOT NULL, `opponent_id` TEXT, `trace_id` TEXT, `transaction_hash` TEXT, `sender` TEXT, `receiver` TEXT, `memo` TEXT, `confirmations` INTEGER, PRIMARY KEY(`snapshot_id`))
                """)
                database.execSQL("""
                    INSERT INTO new_snapshots (`snapshot_id`, `type`, `asset_id`, `amount`, `created_at`, `opponent_id`  , `transaction_hash`, `sender`, `receiver`, `memo`, `confirmations`) 
                    SELECT `snapshot_id`, `type`, `asset_id`, `amount`, `created_at`, `opponent_id`  , `transaction_hash`, `sender`, `receiver`, `memo`, `confirmations` FROM snapshots 
                """)
                database.execSQL(" DROP TABLE IF EXISTS snapshots")
                database.execSQL("ALTER TABLE new_snapshots RENAME TO snapshots")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `new_apps` (`app_id` TEXT NOT NULL, `app_number` TEXT NOT NULL, `home_uri` TEXT NOT NULL, `redirect_uri` TEXT NOT NULL, `name` TEXT NOT NULL, `icon_url` TEXT NOT NULL, `description` TEXT NOT NULL, `app_secret` TEXT NOT NULL, `capabilities` TEXT, `creator_id` TEXT NOT NULL, PRIMARY KEY(`app_id`))
                """)
                database.execSQL("""
                    INSERT INTO new_apps (`app_id`, `app_number`, `home_uri`, `redirect_uri`, `name`, `icon_url`, `description`, `app_secret`, `capabilities`, `creator_id`) 
                    SELECT `app_id`, `app_number`, `home_uri`, `redirect_uri`, `name`, `icon_url`, `description`, `app_secret`, `capabilites`, `creator_id` FROM apps 
                """)
                database.execSQL("DROP TABLE IF EXISTS apps")
                database.execSQL("ALTER TABLE new_apps RENAME TO apps")
                database.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(`content` TEXT, `name` TEXT, tokenize=unicode61, content=`messages`)
                """)
            }
        }

        val MIGRATION_27_28: Migration = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `message_mentions` (`message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `mentions` TEXT NOT NULL, `has_read` INTEGER NOT NULL, PRIMARY KEY(`message_id`))")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_message_mentions_conversation_id` ON `message_mentions` (`conversation_id`)")
                database.execSQL("ALTER TABLE apps ADD COLUMN resource_patterns TEXT")
            }
        }

        val MIGRATION_28_29: Migration = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS index_conversations_created_at")
                database.execSQL("DROP INDEX IF EXISTS index_conversations_conversation_id")
                database.execSQL("ALTER TABLE apps ADD COLUMN updated_at TEXT")
                database.execSQL("DROP TABLE IF EXISTS messages_fts")
                database.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts4` USING FTS4(`message_id` TEXT NOT NULL, `content` TEXT, tokenize=unicode61, notindexed=`message_id`)
                """)
            }
        }
    }
}
