package one.mixin.android.db

import one.mixin.android.Constants
import one.mixin.android.db.datasource.query
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration72Test : BaseMigrationTest() {
    @Test
    fun migrate_71_72_updatesSchema() {
        migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 71).close()

        val migratedDb =
            migrationTestHelper.runMigrationsAndValidate(
                Constants.DataBase.DB_NAME,
                72,
                true,
                MixinDatabaseMigrations.MIGRATION_71_72,
            )

        migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'market_categories'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }

        val indexNames = mutableSetOf<String>()
        migratedDb.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
            while (cursor.moveToNext()) {
                indexNames.add(cursor.getString(0))
            }
        }
        assertTrue(indexNames.contains("index_expired_messages_expire_at"))
        assertFalse(indexNames.contains("index_safe_snapshots_type_asset_id"))
        assertTrue(indexNames.contains("index_safe_snapshots_type_asset_id_created_at"))
    }
}
