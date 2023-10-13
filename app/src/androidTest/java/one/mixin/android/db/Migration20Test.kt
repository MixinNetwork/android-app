package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration20Test : BaseMigrationTest() {

    @Test
    fun migrate_15_20() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            20,
            true,
            MixinDatabaseMigrations.MIGRATION_15_16,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
        )
    }

    @Test
    fun migrate_16_20() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            20,
            true,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
        )
    }

    @Test
    fun migrate_17_20() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            20,
            true,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
        )
    }

    @Test
    fun migrate_18_20() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            20,
            true,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
        )
    }

    @Test
    fun migrate_19_20() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            20,
            true,
            MixinDatabaseMigrations.MIGRATION_19_20,
        )
    }
}
