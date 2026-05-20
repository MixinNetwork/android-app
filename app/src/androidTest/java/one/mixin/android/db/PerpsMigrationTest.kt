package one.mixin.android.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import one.mixin.android.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PerpsMigrationTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PerpsDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate_1_3() {
        migrationTestHelper.createDatabase(Constants.DataBase.PERPS_DB_NAME, 1).close()

        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.PERPS_DB_NAME,
            3,
            true,
            PerpsDatabase.MIGRATION_1_2,
            PerpsDatabase.MIGRATION_2_3,
        )
    }

    @Test
    fun migrate_2_3_preservesDataAndAddsPerpsColumns() {
        migrationTestHelper.createDatabase(Constants.DataBase.PERPS_DB_NAME, 2).apply {
            insertMarketV2()
            insertPositionV2()
            close()
        }

        val migratedDb = migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.PERPS_DB_NAME,
            3,
            true,
            PerpsDatabase.MIGRATION_2_3,
        )

        migratedDb.query(
            "SELECT price_scale, category, tags FROM markets WHERE market_id = 'market-1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("price_scale")))
            assertEquals("major", cursor.getString(cursor.getColumnIndexOrThrow("category")))
            assertEquals("[\"hot\"]", cursor.getString(cursor.getColumnIndexOrThrow("tags")))
        }

        migratedDb.query(
            """
            SELECT take_profit_price, stop_loss_price, liquidation_price, market_id, margin
            FROM positions
            WHERE position_id = 'position-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("take_profit_price")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("stop_loss_price")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("liquidation_price")))
            assertEquals("market-1", cursor.getString(cursor.getColumnIndexOrThrow("market_id")))
            assertEquals("100", cursor.getString(cursor.getColumnIndexOrThrow("margin")))
        }
    }

    @Test
    fun migrate_3_4_replacesPositionHistoriesWithOrders() {
        migrationTestHelper.createDatabase(Constants.DataBase.PERPS_DB_NAME, 3).apply {
            insertPositionHistoryV3()
            close()
        }

        val migratedDb = migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.PERPS_DB_NAME,
            4,
            true,
            PerpsDatabase.MIGRATION_3_4,
        )

        migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'position_histories'",
        ).use { cursor ->
            assertEquals(0, cursor.count)
        }

        migratedDb.execSQL(
            """
            INSERT INTO perps_orders (
                order_id, position_id, market_id, side, order_type, status, quantity, price,
                entry_price, realized_pnl, close_reason, trigger_price, created_at, updated_at
            ) VALUES (
                'order-1', 'position-1', 'market-1', 'long', 'close', 'filled', '1', '100000',
                '99000', '1000', 'take_profit', NULL, '2026-05-15T15:00:00Z', '2026-05-15T15:00:00Z'
            )
            """.trimIndent(),
        )

        migratedDb.query(
            "SELECT realized_pnl, close_reason FROM perps_orders WHERE order_id = 'order-1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("1000", cursor.getString(cursor.getColumnIndexOrThrow("realized_pnl")))
            assertEquals("take_profit", cursor.getString(cursor.getColumnIndexOrThrow("close_reason")))
        }
    }

    private fun SupportSQLiteDatabase.insertMarketV2() {
        execSQL(
            """
            INSERT INTO markets (
                market_id, display_symbol, token_symbol, quote_symbol, mark_price, leverage,
                icon_url, category, tags, funding_rate, min_amount, max_amount, last, volume,
                high, low, open, change, bid_price, ask_price, created_at, updated_at
            ) VALUES (
                'market-1', 'BTC/USDT', 'BTC', 'USDT', '100000', 50,
                'https://example.com/btc.png', 'major', '["hot"]', '0.001', '0.001', '10',
                '99999', '12345', '101000', '98000', '99000', '0.02', '99998', '100001',
                '2026-05-15T15:00:00Z', '2026-05-15T15:00:00Z'
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.insertPositionV2() {
        execSQL(
            """
            INSERT INTO positions (
                position_id, market_id, side, quantity, entry_price, margin, leverage, state,
                mark_price, unrealized_pnl, roe, settle_asset_id, open_pay_amount,
                open_pay_asset_id, bot_id, wallet_id, created_at, updated_at
            ) VALUES (
                'position-1', 'market-1', 'long', '1', '99000', '100', 20, 'open',
                '100000', '10', '0.1', 'asset-1', '100', 'asset-1', 'bot-1', 'wallet-1',
                '2026-05-15T15:00:00Z', '2026-05-15T15:00:00Z'
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.insertPositionHistoryV3() {
        execSQL(
            """
            INSERT INTO position_histories (
                history_id, position_id, market_id, side, quantity, entry_price, close_price,
                realized_pnl, leverage, margin_method, open_at, closed_at
            ) VALUES (
                'history-1', 'position-1', 'market-1', 'long', '1', '99000', '100000',
                '1000', 20, 'cross', '2026-05-15T15:00:00Z', '2026-05-15T16:00:00Z'
            )
            """.trimIndent(),
        )
    }
}
