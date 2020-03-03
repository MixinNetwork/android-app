package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration29Test : BaseMigrationTest() {

    @Test
    fun migrate_15_29() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
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
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_16_29() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
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
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_17_29() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
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
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_18_29() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_18_19,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_19_29() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_20_29() {
        create20()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_21_29() {
        create21()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_22_29() {
        create22()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_23_29() {
        create23()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_24_29() {
        create24()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_25_29() {
        create25()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_26_29() {
        create26()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_27_29() {
        create27()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }

    @Test
    fun migrate_28_29() {
        create28()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME, 29, true,
            MixinDatabaseMigrations.MIGRATION_28_29
        )
    }
}
