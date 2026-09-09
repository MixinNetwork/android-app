package one.mixin.android.db

import android.content.Context
import android.os.Looper
import androidx.lifecycle.Observer
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import one.mixin.android.vo.User
import org.junit.Test
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class UserDaoTest {
    @Test
    fun missingObservedUsersEmitNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()
        try {
            val queries = listOf(
                database.userDao().findUserById("missing-creator"),
                database.userDao().findUserByConversationId("missing-conversation"),
            )
            queries.forEach { result ->
                val observer = Observer<User?> { }
                result.observeForever(observer)
                try {
                    shadowOf(Looper.getMainLooper()).idle()
                    assertTrue("An absent user should emit a value", result.isInitialized)
                    assertNull(result.value)
                } finally {
                    result.removeObserver(observer)
                }
            }
        } finally {
            database.close()
        }
    }
}
