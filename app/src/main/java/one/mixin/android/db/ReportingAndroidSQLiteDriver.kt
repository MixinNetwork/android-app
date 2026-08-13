package one.mixin.android.db

import android.annotation.SuppressLint
import android.database.DatabaseErrorHandler
import android.database.DefaultDatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteConnection
import one.mixin.android.util.reportException
import timber.log.Timber

@SuppressLint("RestrictedApi")
class ReportingAndroidSQLiteDriver(
    private val databaseName: String,
    private val databaseVersion: Int,
) : SQLiteDriver {
    override val hasConnectionPool: Boolean = true

    override fun open(fileName: String): SQLiteConnection {
        val database =
            SQLiteDatabase.openOrCreateDatabase(
                fileName,
                null,
                DatabaseErrorHandler { corruptedDatabase ->
                    try {
                        reportException(IllegalStateException("$databaseName database is corrupted, current DB version: $databaseVersion"))
                    } catch (e: Exception) {
                        Timber.w(e)
                    } finally {
                        DefaultDatabaseErrorHandler().onCorruption(corruptedDatabase)
                    }
                },
            )
        return AndroidSQLiteConnection(database)
    }
}
