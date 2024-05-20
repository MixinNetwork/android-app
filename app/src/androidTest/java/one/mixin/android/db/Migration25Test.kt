package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration25Test : BaseMigrationTest() {
    @Test
    fun migrate_15_25() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_15_16,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_16_25() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_17_25() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_18_25() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_19_25() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_20_25() {
        create20()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_21_25() {
        create21()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_22_25() {
        create22()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_23_25() {
        create23()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }

    @Test
    fun migrate_24_25() {
        create24()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            25,
            true,
            MixinDatabaseMigrations.MIGRATION_24_25,
        )
    }
}
