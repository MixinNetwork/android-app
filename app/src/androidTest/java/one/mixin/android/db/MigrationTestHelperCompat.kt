package one.mixin.android.db

import androidx.room3.migration.Migration
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import kotlinx.coroutines.runBlocking

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
