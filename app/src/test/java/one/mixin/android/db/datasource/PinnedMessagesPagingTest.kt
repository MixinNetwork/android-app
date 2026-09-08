package one.mixin.android.db.datasource

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.DataProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class PinnedMessagesPagingTest {
    @Test
    fun loadPreservesConversationArgumentAndAddsPagination() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()
        try {
            val source = DataProvider.getPinMessages(database, "conversation", 0)
            val result = source.load(PagingSource.LoadParams.Refresh(null, 20, false))
            val page = assertIs<PagingSource.LoadResult.Page<Int, *>>(result)
            assertEquals(emptyList(), page.data)
            source.invalidate()
        } finally {
            database.close()
        }
    }
}
