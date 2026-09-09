package one.mixin.android.db

import android.content.Context
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NullableDaoQueriesTest {
    @Test
    fun missingObservedRowsEmitNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()
        try {
            val queries: List<Pair<String, LiveData<out Any?>>> = listOf(
                "user" to database.userDao().findUserById("missing"),
                "conversation user" to database.userDao().findUserByConversationId("missing"),
                "address" to database.addressDao().observeById("missing"),
                "conversation" to database.conversationDao().getConversationById("missing"),
                "circle" to database.circleDao().observeCirclesByConversationId("missing"),
                "asset" to database.assetDao().asset("missing"),
                "asset item" to database.assetDao().assetItem("missing"),
                "token" to database.tokenDao().asset("missing"),
                "token item" to database.tokenDao().assetItem("missing"),
                "sticker" to database.stickerDao().observeStickerById("missing"),
                "sticker album" to database.stickerAlbumDao().observeAlbumById("missing"),
                "system sticker album" to database.stickerAlbumDao().observeSystemAlbumById("missing"),
            )
            val observer = Observer<Any?> { }
            try {
                queries.forEach { (_, result) -> result.observeForever(observer) }
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals("Queries must emit null for absent rows", emptyList<String>(), queries.filterNot { it.second.isInitialized }.map { it.first })
                queries.forEach { (name, result) -> assertNull(name, result.value) }
            } finally {
                queries.forEach { (_, result) -> result.removeObserver(observer) }
            }
        } finally {
            database.close()
        }
    }
}
