package one.mixin.android.db

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import one.mixin.android.Constants
import one.mixin.android.Constants.DataBase.CURRENT_VERSION
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_15_16
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_16_17
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_17_18
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_18_19
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_19_20
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_20_21
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_21_22
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_22_23
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_23_24
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_24_25
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_25_26
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_26_27
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_27_28
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_28_29
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_29_30
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_30_31
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_31_32
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_32_33
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_33_34
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_34_35
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_35_36
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_36_37
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_37_38
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_38_39
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_39_40
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_40_41
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_41_42
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_42_43
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_43_44
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_44_45
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_45_46
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_46_47
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_47_48
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_48_49
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_49_50
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_50_51
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_51_52
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_52_53
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_53_54
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_54_55
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_55_56
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_56_57
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_57_58
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_58_59
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_59_60
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_60_61
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_61_62
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_62_63
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_63_64
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_64_65
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_65_66
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_66_67
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_67_68
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_68_69
import org.junit.After
import org.junit.Before
import org.junit.Rule

open class BaseMigrationTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            requireNotNull(MixinDatabase::class.java.canonicalName),
            FrameworkSQLiteOpenHelperFactory(),
        )

    protected val allMixinMigrations =
        arrayOf<Migration>(
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
            MIGRATION_24_25,
            MIGRATION_25_26,
            MIGRATION_26_27,
            MIGRATION_27_28,
            MIGRATION_28_29,
            MIGRATION_29_30,
            MIGRATION_30_31,
            MIGRATION_31_32,
            MIGRATION_32_33,
            MIGRATION_33_34,
            MIGRATION_34_35,
            MIGRATION_35_36,
            MIGRATION_36_37,
            MIGRATION_37_38,
            MIGRATION_38_39,
            MIGRATION_39_40,
            MIGRATION_40_41,
            MIGRATION_41_42,
            MIGRATION_42_43,
            MIGRATION_43_44,
            MIGRATION_44_45,
            MIGRATION_45_46,
            MIGRATION_46_47,
            MIGRATION_47_48,
            MIGRATION_48_49,
            MIGRATION_49_50,
            MIGRATION_50_51,
            MIGRATION_51_52,
            MIGRATION_52_53,
            MIGRATION_53_54,
            MIGRATION_54_55,
            MIGRATION_55_56,
            MIGRATION_56_57,
            MIGRATION_57_58,
            MIGRATION_58_59,
            MIGRATION_59_60,
            MIGRATION_60_61,
            MIGRATION_61_62,
            MIGRATION_62_63,
            MIGRATION_63_64,
            MIGRATION_64_65,
            MIGRATION_65_66,
            MIGRATION_66_67,
            MIGRATION_67_68,
            MIGRATION_68_69,
        )

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    fun create15() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 15)
        db.close()
    }

    protected fun createDatabase(version: Int, dbName: String = Constants.DataBase.DB_NAME) {
        val db = migrationTestHelper.createDatabase(dbName, version)
        db.close()
    }

    protected fun runMixinMigrationsAndValidate(
        fromVersion: Int,
        toVersion: Int = CURRENT_VERSION,
    ) {
        val migrations =
            allMixinMigrations
                .filter { it.startVersion >= fromVersion && it.endVersion <= toVersion }
                .sortedBy { it.startVersion }
                .toTypedArray()

        migrationTestHelper.runMigrationsAndValidate(
            Constants.DataBase.DB_NAME,
            toVersion,
            true,
            *migrations,
        )
    }

    fun create16() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 16)
        db.close()
    }

    fun create17() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 17)
        db.close()
    }

    fun create18() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 18)
        db.close()
    }

    fun create19() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 19)
        db.close()
    }

    fun create20() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 20)
        db.close()
    }

    fun create21() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 21)
        db.close()
    }

    fun create22() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 22)
        db.close()
    }

    fun create23() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 23)
        db.close()
    }

    fun create24() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 24)
        db.close()
    }

    fun create25() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 25)
        db.close()
    }

    fun create26() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 26)
        db.close()
    }

    fun create27() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 27)
        db.close()
    }

    fun create28() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 28)
        db.close()
    }

    fun create29() {
        val db = migrationTestHelper.createDatabase(Constants.DataBase.DB_NAME, 29)
        db.close()
    }
}
