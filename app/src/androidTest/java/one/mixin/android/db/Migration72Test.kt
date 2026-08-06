package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration72Test : BaseMigrationTest() {
    @Test
    fun migrate_71_72_createsMarketCategories() {
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
    }
}
