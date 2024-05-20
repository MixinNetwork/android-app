package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration26Test : BaseMigrationTest() {
    @Test
    fun migrate_15_26() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
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
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_16_26() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
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
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_17_26() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_18_26() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_19_26() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_20_26() {
        create20()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_21_26() {
        create21()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_22_26() {
        create22()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_23_26() {
        create23()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_24_26() {
        create24()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }

    @Test
    fun migrate_25_26() {
        create25()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            26,
            true,
            MixinDatabaseMigrations.MIGRATION_25_26,
        )
    }
}
