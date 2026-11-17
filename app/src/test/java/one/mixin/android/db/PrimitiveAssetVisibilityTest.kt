package one.mixin.android.db

import android.content.Context
import android.os.CancellationSignal
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import one.mixin.android.ui.transfer.TransferInserter
import one.mixin.android.ui.wallet.sortQueryAsset
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.safe.toSearchUpdate
import org.robolectric.Shadows.shadowOf
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
            .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Unconfined).allowMainThreadQueries().build()
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
            assertEquals(visibleIds, dao.allAssetItems().map { it.assetId }.toSet())
            assertEquals(visibleIds, dao.findTokenItems(listOf("normal", "empty", "primitive")).map { it.assetId }.toSet())
            assertEquals(visibleIds, dao.assetFlow().first().map { it.assetId }.toSet())
            assertEquals(visibleIds, dao.assetItemsWithBalance().read().map { it.assetId }.toSet())
            assertEquals(visibleIds, dao.assetItems().read().map { it.assetId }.toSet())
            assertEquals(40.0, dao.walletHomeTokenSummary().read().totalUsd)
            assertEquals("underlying", dao.findAssetItemById("primitive")?.primitiveAssetId)
            assertEquals("2", dao.findAssetItemById("primitive")?.balance)
            dao.updatePrimitiveAssetId("primitive", null)
            assertEquals(3, dao.findAssetItemsWithBalance().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun transferAndSearchRetainMarkersWhileUpdatingTokenPrices() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        try {
            val token = one.mixin.android.vo.safe.Token("asset", "kernel", "SYM", "name", "", "1", "10", "chain", "0", "0", 1, "", "0", null, 8, "underlying")
            TransferInserter(database).insertIgnore(token)
            val dao = database.tokenDao()
            val local = requireNotNull(dao.findAssetItemById("asset"))
            assertEquals("underlying", local.primitiveAssetId)
            assertEquals(emptyList(), sortQueryAsset("SYM", listOf(local.copy(primitiveAssetId = null)), listOf(local)))
            val search = local.copy(priceUsd = "20", primitiveAssetId = null)
            dao.updateSearchResults(listOf(search.toSearchUpdate(local.primitiveAssetId)))
            assertEquals("underlying", dao.findAssetItemById("asset")?.primitiveAssetId)
            assertEquals("20", dao.findAssetItemById("asset")?.priceUsd)
            dao.suspendUpdatePrices(listOf(PriceAndChange("asset", "2", "30", "1", "1")))
            assertEquals("30", dao.findAssetItemById("asset")?.priceUsd)
            assertEquals("underlying", dao.findAssetItemById("asset")?.primitiveAssetId)
            dao.updateSearchResults(listOf(search.copy(primitiveAssetId = "").toSearchUpdate(local.primitiveAssetId)))
            assertEquals("", dao.findAssetItemById("asset")?.primitiveAssetId)
        } finally {
            database.close()
        }
    }

    private fun <T> LiveData<T>.read(): T {
        val observer = Observer<T> {}
        observeForever(observer)
        try {
            shadowOf(Looper.getMainLooper()).idle()
            return requireNotNull(value)
        } finally {
            removeObserver(observer)
        }
    }

}
