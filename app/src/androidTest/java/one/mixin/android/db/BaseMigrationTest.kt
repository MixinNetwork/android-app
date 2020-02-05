package one.mixin.android.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import one.mixin.android.Constants
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class BaseMigrationTest {

    @get: Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MixinDatabase::class.java.canonicalName, FrameworkSQLiteOpenHelperFactory()
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
}
