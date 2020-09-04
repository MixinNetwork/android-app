package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration18Test : BaseMigrationTest() {

    @Test
    fun migrate_15_18() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            18,
            true,
            MixinDatabaseMigrations.MIGRATION_15_16,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18
        )
    }

    @Test
    fun migrate_16_18() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            18,
            true,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18
        )
    }

    @Test
    fun migrate_17_18() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            18,
            true,
            MixinDatabaseMigrations.MIGRATION_17_18
        )
    }
}
