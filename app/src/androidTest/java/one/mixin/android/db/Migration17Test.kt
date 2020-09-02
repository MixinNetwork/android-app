package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration17Test : BaseMigrationTest() {

    @Test
    fun migrate_15_17() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            17,
            true,
            MixinDatabaseMigrations.MIGRATION_15_16,
            MixinDatabaseMigrations.MIGRATION_16_17
        )
    }

    @Test
    fun migrate_16_17() {
        create16()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            17,
            true,
            MixinDatabaseMigrations.MIGRATION_16_17
        )
    }
}
