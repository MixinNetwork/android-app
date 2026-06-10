package one.mixin.android.db

import one.mixin.android.Constants.DataBase.CURRENT_VERSION
import one.mixin.android.Constants.DataBase.MINI_VERSION
import org.junit.Test

class MigrationCurrentVersionTest : BaseMigrationTest() {
    @Test
    fun migrate_all_historical_versions_to_current() {
        for (fromVersion in MINI_VERSION until CURRENT_VERSION) {
            try {
                createDatabase(fromVersion)
                runMixinMigrationsAndValidate(fromVersion)
            } catch (t: Throwable) {
                throw AssertionError("Failed to migrate MixinDatabase from $fromVersion to $CURRENT_VERSION", t)
            }
        }
    }
}
