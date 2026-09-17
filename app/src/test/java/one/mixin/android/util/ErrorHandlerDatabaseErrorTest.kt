package one.mixin.android.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import one.mixin.android.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ErrorHandlerDatabaseErrorTest {
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = object : Resources(context.assets, context.resources.displayMetrics, context.resources.configuration) {
            override fun getString(id: Int): String = when (id) {
                R.string.Data_error -> "Data error"
                R.string.error_connection_timeout -> "Timeout"
                R.string.No_network_connection -> "No network"
                R.string.error_unknown_with_message -> "Error: %s"
                else -> error("Unexpected string resource: $id")
            }

            override fun getString(id: Int, vararg formatArgs: Any?): String = getString(id).format(*formatArgs)
        }
        MixinApplication.appContext = object : ContextWrapper(context) {
            override fun getResources(): Resources = resources
        }
    }

    @Test
    fun sqlExceptionsNeverExposeTableOrQuery() {
        for (error in listOf(SQLiteException(SQL_ERROR), SQLiteConstraintException(SQL_ERROR), SQLException(SQL_ERROR), SQLiteException())) {
            assertEquals("Data error", ErrorHandler.getErrorMessage(error))
        }
    }

    @Test
    fun wrappedSqlExceptionsNeverExposeTableOrQuery() {
        for (error in listOf(RuntimeException(SQLiteException(SQL_ERROR)), ExecutionException(SQLiteException(SQL_ERROR)))) {
            assertEquals("Data error", ErrorHandler.getErrorMessage(error))
        }
    }

    @Test
    fun nonDatabaseErrorsKeepTheirExistingMessages() {
        assertEquals("Timeout", ErrorHandler.getErrorMessage(SocketTimeoutException()))
        assertEquals("No network", ErrorHandler.getErrorMessage(UnknownHostException()))
        assertEquals("Error: insufficient balance", ErrorHandler.getErrorMessage(IllegalArgumentException("insufficient balance")))
    }

    companion object {
        private const val SQL_ERROR = "table raw_transactions has no column named hash while compiling INSERT OR REPLACE INTO raw_transactions"
    }
}
