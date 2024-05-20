package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration30Test : BaseMigrationTest() {
    @Test
    fun migrate_15_30() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
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
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_16_30() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
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
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_17_30() {
        create17()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
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
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_18_30() {
        create18()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
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
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_19_30() {
        create19()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_19_20,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_20_30() {
        create20()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_20_21,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_21_30() {
        create21()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_21_22,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_22_30() {
        create22()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_22_23,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_23_30() {
        create23()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_23_24,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_24_30() {
        create24()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_24_25,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_25_30() {
        create25()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_25_26,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_26_30() {
        create26()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_26_27,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_27_30() {
        create27()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_27_28,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_28_30() {
        create28()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_28_29,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }

    @Test
    fun migrate_29_30() {
        create29()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            30,
            true,
            MixinDatabaseMigrations.MIGRATION_29_30,
        )
    }
}
