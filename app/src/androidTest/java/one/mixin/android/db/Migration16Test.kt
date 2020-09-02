package one.mixin.android.db

import one.mixin.android.Constants
import org.junit.Test

class Migration16Test : BaseMigrationTest() {

    @Test
    fun migrate_15_16() {
        create15()
        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            16,
            true,
            MixinDatabaseMigrations.MIGRATION_15_16
        )
    }
}
