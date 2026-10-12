package one.mixin.android.db

import android.content.Context
import android.os.CancellationSignal
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.provider.DataProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PrimitiveAssetVisibilityTest {
    @Test
    fun persistedMarkerHidesTokensWithoutChangingBalances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        try {
            val dao = database.tokenDao()
            for (id in listOf("normal", "empty", "primitive")) {
                RoomDatabaseCompat.execute(database, "INSERT INTO tokens(asset_id, kernel_asset_id, symbol, name, icon_url, price_btc, price_usd, chain_id, change_usd, change_btc, confirmations, asset_key, dust, precision) VALUES('$id', '$id', '$id', 'matching token', '', '0', '10', 'chain', '0', '0', 1, '', '0', 8)")
                RoomDatabaseCompat.execute(database, "INSERT INTO tokens_extra(asset_id, kernel_asset_id, hidden, balance, updated_at) VALUES('$id', '$id', 0, '2', '')")
            }
            dao.updatePrimitiveAssetId("empty", "")
            dao.updatePrimitiveAssetId("primitive", "underlying")
            val visibleIds = setOf("normal", "empty")
            assertEquals(visibleIds, dao.findAssetItemsWithBalance().map { it.assetId }.toSet())
            assertEquals(visibleIds, dao.fuzzySearchAssetIgnoreAmount("matching", "matching").map { it.assetId }.toSet())
            assertEquals(visibleIds, DataProvider.fuzzySearchToken("matching", "matching", database, CancellationSignal()).map { it.assetId }.toSet())
            assertEquals(visibleIds, dao.findUnifiedAssetItem().map { it.symbol }.toSet())
            assertEquals("underlying", dao.findAssetItemById("primitive")?.primitiveAssetId)
            assertEquals("2", dao.findAssetItemById("primitive")?.balance)
            dao.updatePrimitiveAssetId("primitive", null)
            assertEquals(3, dao.findAssetItemsWithBalance().size)
        } finally {
            database.close()
        }
    }
}
