package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration28Test : BaseMigrationTest() {

    @Test
    fun migrate_15_28() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
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
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_16_28() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
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
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_17_28() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_17_18,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_18_28() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_19_28() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_20_28() {
        create20()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_21_28() {
        create21()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_22_28() {
        create22()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_23_28() {
        create23()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_24_28() {
        create24()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_25_28() {
        create25()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_26_28() {
        create26()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }

    @Test
    fun migrate_27_28() {
        create27()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 28, true,
            MixinDatabaseMigrations.MIGRATION_27_28
        )
    }
}
