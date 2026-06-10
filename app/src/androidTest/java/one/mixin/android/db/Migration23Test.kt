package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration23Test : BaseMigrationTest() {
    @Test
    fun migrate_15_23() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_15_16,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_16_23() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_16_17,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_17_23() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_18_23() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_19_23() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_20_23() {
        create20()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_21_23() {
        create21()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }

    @Test
    fun migrate_22_23() {
        create22()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            23,
            true,
            MixinDatabaseMigrations.MIGRATION_22_23,
        )
    }
}
