package one.mixin.android.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import one.mixin.android.Constants.DataBase.PERPS_DB_NAME
import org.junit.Rule
import org.junit.Test

class PerpsMigrationTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            requireNotNull(PerpsDatabase::class.java.canonicalName),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate_1_2() {
        val db = migrationTestHelper.createDatabase(PERPS_DB_NAME, 1)
        db.close()

        migrationTestHelper.runMigrationsAndValidate(
            PERPS_DB_NAME,
            2,
            true,
            PerpsDatabase.MIGRATION_1_2,
        )
    }
}
