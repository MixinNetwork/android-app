package one.mixin.android.db

import androidx.room3.migration.Migration
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import java.io.File

// Room 3 never deletes the helper's file, and tests must not touch the app's real database
fun freshMigrationTestDatabase(name: String): File {
    deleteMigrationTestDatabase(name)
    return InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath("migration-test-$name")
}

fun deleteMigrationTestDatabase(name: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    context.deleteDatabase("migration-test-$name")
    File("${context.getDatabasePath("migration-test-$name").path}.lck").delete()
}

fun MigrationTestHelper.createDatabase(
    name: String,
    version: Int,
): SQLiteConnection = runBlocking { createDatabase(version) }

fun MigrationTestHelper.runMigrationsAndValidate(
    name: String,
    version: Int,
    validateDroppedTables: Boolean,
    vararg migrations: Migration,
): SQLiteConnection = runBlocking { runMigrationsAndValidate(version, migrations.toList()) }
