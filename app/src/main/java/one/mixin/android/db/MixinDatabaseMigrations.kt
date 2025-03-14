package one.mixin.android.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.mixin.android.Constants.DataBase.MINI_VERSION
import one.mixin.android.extension.nowInUtc
import one.mixin.android.session.Session

class MixinDatabaseMigrations private constructor() {
    companion object {
        val MIGRATION_15_16: Migration =
            object : Migration(MINI_VERSION, 16) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS assets")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                            "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                            "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, hidden INTEGER, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT) ",
                    )
                    db.execSQL("DROP TABLE IF EXISTS addresses")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS addresses(address_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, " +
                            "public_key TEXT, label TEXT, updated_at TEXT NOT NULL, reserve TEXT NOT NULL, fee TEXT NOT NULL, account_name TEXT, account_tag TEXT)",
                    )
                }
            }

        val MIGRATION_16_17: Migration =
            object : Migration(16, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS jobs (job_id TEXT NOT NULL, action TEXT NOT NULL, created_at TEXT NOT NULL, order_id INTEGER, priority " +
                            "INTEGER NOT NULL, user_id TEXT, blaze_message TEXT, conversation_id TEXT, resend_message_id TEXT, run_count INTEGER NOT NULL, PRIMARY KEY" +
                            "(job_id))",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_messages_conversation_id_user_id_status_created_at ON messages (conversation_id, user_id, " +
                            "status, created_at)",
                    )
                }
            }

        val MIGRATION_17_18: Migration =
            object : Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE addresses ADD COLUMN dust TEXT")
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_update")
                }
            }

        val MIGRATION_18_19: Migration =
            object : Migration(18, 19) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE snapshots ADD COLUMN confirmations INTEGER")
                }
            }

        val MIGRATION_19_20: Migration =
            object : Migration(19, 20) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS top_assets")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS top_assets(asset_id TEXT PRIMARY KEY NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, " +
                            "icon_url TEXT NOT NULL, balance TEXT NOT NULL, public_key TEXT, price_btc TEXT NOT NULL, price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, " +
                            "change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, confirmations INTEGER NOT NULL, account_name TEXT, account_tag TEXT, capitalization REAL) ",
                    )
                }
            }

        val MIGRATION_20_21: Migration =
            object : Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE assets ADD COLUMN asset_key TEXT")
                }
            }

        val MIGRATION_21_22: Migration =
            object : Migration(21, 22) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE messages ADD COLUMN thumb_url TEXT")
                }
            }

        val MIGRATION_22_23: Migration =
            object : Migration(22, 23) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE users ADD COLUMN biography TEXT NOT NULL DEFAULT ''")
                }
            }

        val MIGRATION_23_24: Migration =
            object : Migration(23, 24) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS addresses")
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS addresses (address_id TEXT NOT NULL, type TEXT NOT NULL, 
                    asset_id TEXT NOT NULL, destination TEXT NOT NULL, label TEXT NOT NULL, updated_at TEXT NOT NULL, 
                    reserve TEXT NOT NULL, fee TEXT NOT NULL, tag TEXT, dust TEXT, PRIMARY KEY(address_id))
                    """,
                    )
                    db.execSQL("CREATE TABLE IF NOT EXISTS assets_extra (asset_id TEXT NOT NULL, hidden INTEGER, PRIMARY KEY(asset_id))")
                    db.execSQL("INSERT OR REPLACE INTO assets_extra (asset_id, hidden) SELECT asset_id, hidden FROM assets WHERE hidden = 1")

                    db.execSQL("DROP TABLE IF EXISTS assets")
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS assets (asset_id TEXT NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, 
                    icon_url TEXT NOT NULL, balance TEXT NOT NULL, destination TEXT NOT NULL, tag TEXT, price_btc TEXT NOT NULL, 
                    price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, 
                    confirmations INTEGER NOT NULL, asset_key TEXT, PRIMARY KEY(asset_id))
                    """,
                    )
                    db.execSQL("DROP TABLE IF EXISTS top_assets")
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS top_assets (asset_id TEXT NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, 
                    icon_url TEXT NOT NULL, balance TEXT NOT NULL, destination TEXT NOT NULL, tag TEXT, price_btc TEXT NOT NULL, 
                    price_usd TEXT NOT NULL, chain_id TEXT NOT NULL, change_usd TEXT NOT NULL, change_btc TEXT NOT NULL, 
                    confirmations INTEGER NOT NULL, PRIMARY KEY(asset_id))
                    """,
                    )
                }
            }

        val MIGRATION_24_25: Migration =
            object : Migration(24, 25) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS participant_session (`conversation_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `sent_to_server` INTEGER, `created_at` TEXT, PRIMARY KEY(`conversation_id`, `user_id`, `session_id`))",
                    )
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_insert")
                }
            }

        val MIGRATION_25_26: Migration =
            object : Migration(25, 26) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS favorite_apps (`app_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`app_id`, `user_id`))",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS resend_session_messages (`message_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `status` INTEGER NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`message_id`, `user_id`, `session_id`))",
                    )
                }
            }

        val MIGRATION_26_27: Migration =
            object : Migration(26, 27) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS `new_snapshots` (`snapshot_id` TEXT NOT NULL, `type` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `amount` TEXT NOT NULL, `created_at` TEXT NOT NULL, `opponent_id` TEXT, `trace_id` TEXT, `transaction_hash` TEXT, `sender` TEXT, `receiver` TEXT, `memo` TEXT, `confirmations` INTEGER, PRIMARY KEY(`snapshot_id`))
                    """,
                    )
                    db.execSQL(
                        """
                    INSERT INTO new_snapshots (`snapshot_id`, `type`, `asset_id`, `amount`, `created_at`, `opponent_id`  , `transaction_hash`, `sender`, `receiver`, `memo`, `confirmations`) 
                    SELECT `snapshot_id`, `type`, `asset_id`, `amount`, `created_at`, `opponent_id`  , `transaction_hash`, `sender`, `receiver`, `memo`, `confirmations` FROM snapshots 
                    """,
                    )
                    db.execSQL(" DROP TABLE IF EXISTS snapshots")
                    db.execSQL("ALTER TABLE new_snapshots RENAME TO snapshots")
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS `new_apps` (`app_id` TEXT NOT NULL, `app_number` TEXT NOT NULL, `home_uri` TEXT NOT NULL, `redirect_uri` TEXT NOT NULL, `name` TEXT NOT NULL, `icon_url` TEXT NOT NULL, `description` TEXT NOT NULL, `app_secret` TEXT NOT NULL, `capabilities` TEXT, `creator_id` TEXT NOT NULL, PRIMARY KEY(`app_id`))
                    """,
                    )
                    db.execSQL(
                        """
                    INSERT INTO new_apps (`app_id`, `app_number`, `home_uri`, `redirect_uri`, `name`, `icon_url`, `description`, `app_secret`, `capabilities`, `creator_id`) 
                    SELECT `app_id`, `app_number`, `home_uri`, `redirect_uri`, `name`, `icon_url`, `description`, `app_secret`, `capabilites`, `creator_id` FROM apps 
                    """,
                    )
                    db.execSQL("DROP TABLE IF EXISTS apps")
                    db.execSQL("ALTER TABLE new_apps RENAME TO apps")
                    db.execSQL(
                        """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(`content` TEXT, `name` TEXT, tokenize=unicode61, content=`messages`)
                    """,
                    )
                }
            }

        val MIGRATION_27_28: Migration =
            object : Migration(27, 28) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `message_mentions` (`message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `mentions` TEXT NOT NULL, `has_read` INTEGER NOT NULL, PRIMARY KEY(`message_id`))",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_mentions_conversation_id` ON `message_mentions` (`conversation_id`)")
                    db.execSQL("ALTER TABLE apps ADD COLUMN resource_patterns TEXT")
                }
            }

        val MIGRATION_28_29: Migration =
            object : Migration(28, 29) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_conversations_created_at")
                    db.execSQL("DROP INDEX IF EXISTS index_conversations_conversation_id")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_snapshots_asset_id` ON `snapshots` (`asset_id`)")
                    db.execSQL("ALTER TABLE apps ADD COLUMN updated_at TEXT")
                    db.execSQL("DROP TABLE IF EXISTS messages_fts")
                    db.execSQL(
                        """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts4` USING FTS4(`message_id` TEXT NOT NULL, `content` TEXT, tokenize=unicode61, notindexed=`message_id`)
                    """,
                    )
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_count_insert")
                }
            }

        val MIGRATION_29_30: Migration =
            object : Migration(29, 30) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `circles` (`circle_id` TEXT NOT NULL, `name` TEXT NOT NULL, `created_at` TEXT NOT NULL, `ordered_at` TEXT, PRIMARY KEY(`circle_id`))",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `circle_conversations` (`conversation_id` TEXT NOT NULL, `circle_id` TEXT NOT NULL, `user_id` TEXT, `created_at` TEXT NOT NULL, `pin_time` TEXT, PRIMARY KEY(`conversation_id`, `circle_id`))",
                    )
                }
            }

        val MIGRATION_30_31: Migration =
            object : Migration(30, 31) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE assets ADD COLUMN reserve TEXT")
                    db.execSQL("ALTER TABLE apps ADD COLUMN category TEXT")
                }
            }

        val MIGRATION_31_32: Migration =
            object : Migration(31, 32) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE users ADD COLUMN is_scam INTEGER")
                }
            }

        val MIGRATION_32_33: Migration =
            object : Migration(32, 33) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `traces` (`trace_id` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `amount` TEXT NOT NULL, 
                        `opponent_id` TEXT, `destination` TEXT, `tag` TEXT, `snapshot_id` TEXT, `created_at` TEXT NOT NULL, 
                        PRIMARY KEY(`trace_id`))
                    """,
                    )
                }
            }

        val MIGRATION_33_34: Migration =
            object : Migration(33, 34) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE participant_session ADD COLUMN public_key TEXT")
                    db.execSQL("ALTER TABLE messages ADD COLUMN caption TEXT")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversation_id_status_user_id` ON `messages` (`conversation_id`, `status`, `user_id`)")
                    db.execSQL("DROP INDEX IF EXISTS `index_messages_user_id`")
                }
            }

        val MIGRATION_34_35: Migration =
            object : Migration(34, 35) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_jobs_action` ON `jobs` (`action`)")
                }
            }

        val MIGRATION_35_36: Migration =
            object : Migration(35, 36) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE conversations ADD COLUMN last_message_created_at TEXT")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_pin_time_last_message_created_at` ON `conversations` (`pin_time`, `last_message_created_at`)")
                    db.execSQL("UPDATE conversations SET last_message_created_at = (SELECT created_at FROM messages WHERE id = conversations.last_message_id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_relationship_full_name` ON `users` (`relationship`, `full_name`)")
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_last_message_update")
                }
            }

        val MIGRATION_36_37: Migration =
            object : Migration(36, 37) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS `index_participants_conversation_id`")
                    db.execSQL("DROP INDEX IF EXISTS `index_participants_created_at`")
                    db.execSQL("DROP INDEX IF EXISTS `index_users_full_name`")
                    db.execSQL("DROP INDEX IF EXISTS `index_snapshots_asset_id`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversation_id_category` ON messages(`conversation_id`, `category`);")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversation_id_quote_message_id` ON `messages` (`conversation_id`, `quote_message_id`)")
                }
            }

        val MIGRATION_37_38: Migration =
            object : Migration(37, 38) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `transcript_messages` (`transcript_id` TEXT NOT NULL, `message_id` TEXT NOT NULL, `user_id` TEXT, `user_full_name` TEXT, `category` TEXT NOT NULL, `created_at` TEXT NOT NULL, `content` TEXT, `media_url` TEXT, `media_name` TEXT, `media_size` INTEGER, `media_width` INTEGER, `media_height` INTEGER, `media_mime_type` TEXT, `media_duration` INTEGER, `media_status` TEXT, `media_waveform` BLOB, `thumb_image` TEXT, `thumb_url` TEXT, `media_key` BLOB, `media_digest` BLOB, `media_created_at` TEXT, `sticker_id` TEXT, `shared_user_id` TEXT, `mentions` TEXT, `quote_id` TEXT, `quote_content` TEXT, `caption` TEXT, PRIMARY KEY(`transcript_id`, `message_id`))")
                }
            }

        val MIGRATION_38_39: Migration =
            object : Migration(38, 39) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `pin_messages` (`message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`message_id`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_pin_messages_conversation_id` ON `pin_messages` (`conversation_id`)")
                    db.execSQL("DROP INDEX IF EXISTS `index_messages_conversation_id_user_id_status_created_at`")
                    db.execSQL("DROP INDEX IF EXISTS `index_messages_conversation_id_status_user_id`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversation_id_status_user_id_created_at` ON `messages` (`conversation_id`, `status`,`user_id`, `created_at`)")
                }
            }

        val MIGRATION_39_40: Migration =
            object : Migration(39, 40) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `properties` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`key`))")
                }
            }

        val MIGRATION_40_41: Migration =
            object : Migration(40, 41) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE sticker_albums ADD COLUMN banner TEXT")
                    db.execSQL("ALTER TABLE sticker_albums ADD COLUMN ordered_at INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE sticker_albums ADD COLUMN added INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE sticker_albums SET added = 1")
                }
            }

        val MIGRATION_41_42: Migration =
            object : Migration(41, 42) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE sticker_albums ADD COLUMN is_verified INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE sticker_albums SET is_verified = 1 WHERE category = 'SYSTEM'")
                }
            }

        val MIGRATION_42_43: Migration =
            object : Migration(42, 43) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `remote_messages_status` (`message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`message_id`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_remote_messages_status_conversation_id_status` ON `remote_messages_status` (`conversation_id`, `status`)")
                    Session.getAccountId()?.let { selfId ->
                        db.query("SELECT conversation_id FROM conversations").use { c ->
                            val conversationIds = mutableListOf<String>()
                            while (c.moveToNext()) {
                                val cid = c.getString(0)
                                conversationIds.add("'$cid'")
                            }
                            conversationIds.chunked(99).forEach { ids ->
                                db.execSQL("INSERT OR REPLACE INTO remote_messages_status(message_id, conversation_id, status) SELECT id, conversation_id, 'DELIVERED' FROM messages WHERE conversation_id IN (${ids.joinToString()}) AND status IN ('DELIVERED','SENT') AND user_id != '$selfId'")
                            }
                        }
                    }
                }
            }

        val MIGRATION_43_44: Migration =
            object : Migration(43, 44) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `conversations` ADD COLUMN `expire_in` INTEGER")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `expired_messages` (`message_id` TEXT NOT NULL, `expire_in` INTEGER NOT NULL, `expire_at` INTEGER, PRIMARY KEY(`message_id`))")
                }
            }

        val MIGRATION_44_45: Migration =
            object : Migration(44, 45) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `assets` ADD COLUMN `deposit_entries` TEXT")
                }
            }

        val MIGRATION_45_46: Migration =
            object : Migration(45, 46) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `addresses` ADD COLUMN fee_asset_id TEXT NOT NULL DEFAULT ''")
                }
            }

        val MIGRATION_46_47: Migration =
            object : Migration(46, 47) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `conversation_ext` (`conversation_id` TEXT NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, `created_at` TEXT NOT NULL, PRIMARY KEY(`conversation_id`))")
                    db.execSQL("INSERT OR REPLACE INTO `conversation_ext` (`conversation_id`, `count`, `created_at`) SELECT conversation_id, count(1), '${nowInUtc()}' FROM messages m INNER JOIN users u ON m.user_id = u.user_id GROUP BY conversation_id LIMIT 100")
                }
            }

        val MIGRATION_47_48: Migration =
            object : Migration(47, 48) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `chains` (`chain_id` TEXT NOT NULL, `name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `icon_url` TEXT NOT NULL, `threshold` INTEGER NOT NULL, PRIMARY KEY(`chain_id`))")
                }
            }

        val MIGRATION_48_49: Migration =
            object : Migration(48, 49) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `snapshots` ADD COLUMN `snapshot_hash` TEXT")
                    db.execSQL("ALTER TABLE `snapshots` ADD COLUMN `opening_balance` TEXT")
                    db.execSQL("ALTER TABLE `snapshots` ADD COLUMN `closing_balance` TEXT")
                }
            }

        val MIGRATION_49_50: Migration =
            object : Migration(49, 50) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `is_deactivated` INTEGER")
                    db.execSQL("ALTER TABLE `assets` ADD COLUMN `withdrawal_memo_possibility` text")
                }
            }

        val MIGRATION_50_51: Migration =
            object : Migration(50, 51) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `outputs` (`output_id` TEXT NOT NULL, `transaction_hash` TEXT NOT NULL, `output_index` INTEGER NOT NULL, `asset` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `amount` TEXT NOT NULL, `mask` TEXT NOT NULL, `keys` TEXT NOT NULL, `receivers` TEXT NOT NULL, `receivers_hash` TEXT NOT NULL, `receivers_threshold` INTEGER NOT NULL, `extra` TEXT NOT NULL, `state` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, `signed_by` TEXT NOT NULL, `signed_at` TEXT NOT NULL, `spent_at` TEXT NOT NULL, PRIMARY KEY(`output_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `tokens` (`asset_id` TEXT NOT NULL, `kernel_asset_id` TEXT NOT NULL, `symbol` TEXT NOT NULL, `name` TEXT NOT NULL, `icon_url` TEXT NOT NULL, `price_btc` TEXT NOT NULL, `price_usd` TEXT NOT NULL, `chain_id` TEXT NOT NULL, `change_usd` TEXT NOT NULL, `change_btc` TEXT NOT NULL, `confirmations` INTEGER NOT NULL, `asset_key` TEXT NOT NULL, `dust` TEXT NOT NULL, PRIMARY KEY(`asset_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `tokens_extra` (`asset_id` TEXT NOT NULL, `kernel_asset_id` TEXT NOT NULL, `hidden` INTEGER, `balance` TEXT, `updated_at` TEXT NOT NULL, PRIMARY KEY(`asset_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `safe_snapshots` (`snapshot_id` TEXT NOT NULL, `type` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `amount` TEXT NOT NULL, `user_id` TEXT NOT NULL, `opponent_id` TEXT NOT NULL, `memo` TEXT NOT NULL, `transaction_hash` TEXT NOT NULL, `created_at` TEXT NOT NULL, `trace_id` TEXT, `confirmations` INTEGER, `opening_balance` TEXT, `closing_balance` TEXT, PRIMARY KEY(`snapshot_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `deposit_entries` (`entry_id` TEXT NOT NULL, `chain_id` TEXT NOT NULL, `destination` TEXT NOT NULL, `members` TEXT NOT NULL, `tag` TEXT, `signature` TEXT NOT NULL, `threshold` INTEGER NOT NULL, `is_primary` INTEGER NOT NULL, PRIMARY KEY(`entry_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `raw_transactions` (`request_id` TEXT NOT NULL, `raw_transaction` TEXT NOT NULL, `receiver_id` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`request_id`))")
                    db.execSQL("ALTER TABLE `chains` ADD COLUMN `withdrawal_memo_possibility` TEXT NOT NULL DEFAULT 'possible'")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_outputs_asset_state_created_at` ON `outputs` (`asset`, `state`, `created_at`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_outputs_transaction_hash_output_index` ON `outputs` (`transaction_hash`, `output_index`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tokens_kernel_asset_id` ON `tokens` (`kernel_asset_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tokens_extra_kernel_asset_id` ON `tokens_extra` (`kernel_asset_id`)")
                }
            }

        val MIGRATION_51_52: Migration =
            object : Migration(51, 52) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `safe_snapshots` ADD COLUMN `deposit` TEXT")
                    db.execSQL("ALTER TABLE `safe_snapshots` ADD COLUMN  `withdrawal` TEXT")
                    db.execSQL("DROP INDEX IF EXISTS `index_outputs_asset_state_created_at`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_outputs_asset_state_sequence` ON `outputs` (`asset`, `state`, `sequence`)")
                }
            }

        val MIGRATION_52_53: Migration =
            object : Migration(52, 53) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `raw_transactions` ADD COLUMN `state` TEXT NOT NULL DEFAULT 'unspent'")
                    db.execSQL("ALTER TABLE `raw_transactions` ADD COLUMN `type` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_raw_transactions_state_type` ON `raw_transactions` (`state`, `type`)")
                }
            }

        val MIGRATION_53_54: Migration =
            object : Migration(53, 54) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `outputs` ADD COLUMN `inscription_hash` TEXT")
                    db.execSQL("ALTER TABLE `tokens` ADD COLUMN `collection_hash` TEXT")
                    db.execSQL("ALTER TABLE `safe_snapshots` ADD COLUMN `inscription_hash` TEXT")
                    db.execSQL("ALTER TABLE `raw_transactions` ADD COLUMN `inscription_hash` TEXT")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_outputs_inscription_hash` ON `outputs` (`inscription_hash`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_tokens_collection_hash` ON `tokens` (`collection_hash`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `inscription_collections` (`collection_hash` TEXT NOT NULL, `supply` TEXT NOT NULL, `unit` TEXT NOT NULL, `symbol` TEXT NOT NULL, `name` TEXT NOT NULL, `icon_url` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`collection_hash`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `inscription_items` (`inscription_hash` TEXT NOT NULL, `collection_hash` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `content_type` TEXT NOT NULL, `content_url` TEXT NOT NULL, `occupied_by` TEXT, `occupied_at` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`inscription_hash`))")
                }
            }

        val MIGRATION_54_55: Migration =
            object : Migration(54, 55) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `inscription_items` ADD COLUMN `traits` TEXT")
                    db.execSQL("ALTER TABLE `inscription_items` ADD COLUMN `owner` TEXT")
                    db.execSQL("ALTER TABLE `inscription_collections` ADD COLUMN `description` TEXT")
                }
            }

        val MIGRATION_55_56: Migration =
            object : Migration(55, 56) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `inscription_collections` ADD COLUMN `kernel_asset_id` TEXT")
                }
            }

        val MIGRATION_56_57: Migration =
            object : Migration(56, 57) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `inscription_collections` ADD COLUMN `treasury` TEXT")
                }
            }

        val MIGRATION_57_58: Migration =
            object : Migration(57, 58) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `markets` (`asset_id` TEXT NOT NULL, `current_price` TEXT NOT NULL, `market_cap` TEXT NOT NULL, `market_cap_rank` TEXT NOT NULL, `total_volume` TEXT NOT NULL, `high_24h` TEXT NOT NULL, `low_24h` TEXT NOT NULL, `price_change_24h` TEXT NOT NULL, `price_change_percentage_24h` TEXT NOT NULL, `market_cap_change_24h` TEXT NOT NULL, `market_cap_change_percentage_24h` TEXT NOT NULL, `circulating_supply` TEXT NOT NULL, `total_supply` TEXT NOT NULL, `max_supply` TEXT NOT NULL, `ath` TEXT NOT NULL, `ath_change_percentage` TEXT NOT NULL, `ath_date` TEXT NOT NULL, `atl` TEXT NOT NULL, `atl_change_percentage` TEXT NOT NULL, `atl_date` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`asset_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `history_prices` (`asset_id` TEXT NOT NULL, `type` TEXT NOT NULL, `data` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`asset_id`, `type`))")
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `membership` TEXT")
                }
            }

        val MIGRATION_58_59: Migration =
            object : Migration(58,59) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS `history_prices`")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `history_prices` (`coin_id` TEXT NOT NULL, `type` TEXT NOT NULL, `data` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`coin_id`, `type`))")
                    db.execSQL("DROP TABLE IF EXISTS `markets`")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `markets` (`coin_id` TEXT NOT NULL, `name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `icon_url` TEXT NOT NULL, `current_price` TEXT NOT NULL, `market_cap` TEXT NOT NULL, `market_cap_rank` TEXT NOT NULL, `total_volume` TEXT NOT NULL, `high_24h` TEXT NOT NULL, `low_24h` TEXT NOT NULL, `price_change_24h` TEXT NOT NULL, `price_change_percentage_1h` TEXT NOT NULL, `price_change_percentage_24h` TEXT NOT NULL, `price_change_percentage_7d` TEXT NOT NULL, `price_change_percentage_30d` TEXT NOT NULL, `market_cap_change_24h` TEXT NOT NULL, `market_cap_change_percentage_24h` TEXT NOT NULL, `circulating_supply` TEXT NOT NULL, `total_supply` TEXT NOT NULL, `max_supply` TEXT NOT NULL, `ath` TEXT NOT NULL, `ath_change_percentage` TEXT NOT NULL, `ath_date` TEXT NOT NULL, `atl` TEXT NOT NULL, `atl_change_percentage` TEXT NOT NULL, `atl_date` TEXT NOT NULL, `asset_ids` TEXT, `sparkline_in_7d` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`coin_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `market_coins` (`asset_id` TEXT NOT NULL, `coin_id` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`asset_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `market_favored` (`coin_id` TEXT NOT NULL, `is_favored` INTEGER NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`coin_id`))")
                }
            }
        
        val MIGRATION_59_60: Migration =
            object : Migration(59, 60) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS new_addresses (address_id TEXT NOT NULL, type TEXT NOT NULL, asset_id TEXT NOT NULL, destination TEXT NOT NULL, label TEXT NOT NULL, updated_at TEXT NOT NULL, tag TEXT, dust TEXT, PRIMARY KEY(address_id))")

                    db.execSQL(
                        """
                        INSERT INTO new_addresses (address_id, type, asset_id, destination, label, updated_at, tag, dust)
                        SELECT address_id, type, asset_id, destination, label, updated_at, tag, dust
                        FROM addresses
                        """
                    )

                    db.execSQL("DROP TABLE IF EXISTS addresses")
                    db.execSQL("ALTER TABLE new_addresses RENAME TO addresses")
                }
            }

        val MIGRATION_60_61: Migration =
            object : Migration(60, 61) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_safe_snapshots_created_at` ON `safe_snapshots` (`created_at`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_safe_snapshots_type_asset_id` ON `safe_snapshots` (`type`, `asset_id`)")
                }
            }

        val MIGRATION_61_62: Migration =
            object : Migration(61, 62) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `market_alerts` (`alert_id` TEXT NOT NULL, `coin_id` TEXT NOT NULL, `type` TEXT NOT NULL, `frequency` TEXT NOT NULL, `status` TEXT NOT NULL, `value` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`alert_id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `market_cap_ranks` (`coin_id` TEXT NOT NULL, `market_cap_rank` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`coin_id`))")
                }
            }

        val MIGRATION_62_63: Migration =
            object : Migration(62, 63) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `markets` ADD COLUMN `sparkline_in_24h` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("DROP INDEX IF EXISTS `index_pin_messages_conversation_id`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_pin_messages_conversation_id_created_at` ON `pin_messages` (`conversation_id`, `created_at`)")
                }
            }

        val MIGRATION_63_64: Migration =
            object : Migration(65, 64) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS `addresses`")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `addresses` (`address_id` TEXT NOT NULL, `type` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `destination` TEXT NOT NULL, `label` TEXT NOT NULL, `updated_at` TEXT NOT NULL, `tag` TEXT, `dust` TEXT, PRIMARY KEY(`address_id`))")
                }
            }


        val MIGRATION_64_65: Migration =
            object : Migration(64, 65) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS `addresses`")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `addresses` (`address_id` TEXT NOT NULL, `type` TEXT NOT NULL, `asset_id` TEXT NOT NULL, `chain_id` TEXT NOT NULL, `destination` TEXT NOT NULL, `label` TEXT NOT NULL, `updated_at` TEXT NOT NULL, `tag` TEXT, `dust` TEXT, PRIMARY KEY(`address_id`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_addresses_chain_id_updated_at` ON `addresses` (`chain_id`, `updated_at`)")
                }
            }
        // If you add a new table, be sure to add a clear method to the DatabaseUtil
    }
}
