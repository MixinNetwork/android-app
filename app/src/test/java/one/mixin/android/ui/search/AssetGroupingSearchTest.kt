package one.mixin.android.ui.search

import android.content.Context
import android.os.CancellationSignal
import androidx.recyclerview.widget.RecyclerView
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssetGroupingSearchTest {
    @Test
    fun homepageQueryPreservesCoinIdsForEveryMatchingNetwork() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), MixinDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            listOf("ethereum", "base", "solana", "polygon", "unknown").forEach { id ->
                database.tokenDao().insertSuspend(
                    Token(id, id, "USDC", "USD Coin", "", "0", "1", id, "0", "0", 0, id, "0", null, 6),
                )
                database.tokensExtraDao().insertSuspend(TokensExtra(id, id, false, "1", "2026-09-08T00:00:00Z"))
                if (id != "unknown") {
                    database.marketCoinDao().insertSuspend(MarketCoin(id, "usd-coin", ""))
                }
            }

            val result = DataProvider.fuzzySearchToken("USDC", "USDC", database, CancellationSignal())

            assertEquals(5, result.size)
            assertTrue(result.filter { it.assetId != "unknown" }.all { it.coinId == "usd-coin" })
            assertEquals(null, result.single { it.assetId == "unknown" }.coinId)
        } finally {
            database.close()
        }
    }

    @Test
    fun groupingRefreshNotifiesRecyclerViewWhenTheVisibleRowCountChanges() {
        val tokens = listOf(token("ethereum", null, "1"), token("base", null, "2"))
        val adapter = SearchAdapter()
        adapter.setData(tokens, null, null)
        adapter.setUrlData("https://example.com")
        var notifiedCount = adapter.itemCount
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                notifiedCount = adapter.itemCount
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                notifiedCount += itemCount
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                notifiedCount -= itemCount
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                assertTrue(positionStart + itemCount <= notifiedCount)
            }
        })

        adapter.setAssetData(tokens.map { it.copy(coinId = "usd-coin") })

        assertEquals(2, adapter.itemCount)
        assertEquals(adapter.itemCount, notifiedCount)
    }

    @Test
    fun moreReceivesCompleteGroupsFromHomepageAndExploreAndRegroupsNewSearches() {
        val tokens = listOf(
            token("ethereum", "usd-coin", "1"), token("base", "usd-coin", "2"),
            token("bitcoin", "bitcoin", "3"), token("solana", "solana", "4"), token("tron", "tron", "5"),
        )
        val homepage = SearchAdapter().apply { setData(tokens, null, null) }
        val explore = SearchExploreAdapter().apply { setAssets(tokens) }

        listOf(homepage.getTypeData(0), explore.getTypeData(0)).forEach { source ->
            val members = requireNotNull(source).filterIsInstance<TokenItem>()
            assertEquals(tokens.map { it.assetId }.toSet(), members.map { it.assetId }.toSet())
            val more = SearchSingleAdapter(TypeAsset).apply { data = members }
            assertEquals(4, more.itemCount)
            assertEquals("1", (more.data!!.first() as TokenItem).balance)

            more.data = tokens.take(2)

            assertEquals(1, more.itemCount)
            assertEquals("1", (more.data!!.single() as TokenItem).balance)
        }
    }

    private fun token(id: String, coin: String?, amount: String) = TokenItem(
        assetId = id,
        symbol = "USDC",
        name = "USD Coin",
        iconUrl = "",
        balance = amount,
        priceBtc = "0",
        priceUsd = "1",
        chainId = id,
        changeUsd = "0",
        changeBtc = "0",
        hidden = false,
        confirmations = 0,
        chainIconUrl = null,
        chainSymbol = null,
        chainName = null,
        assetKey = id,
        dust = null,
        withdrawalMemoPossibility = null,
        collectionHash = null,
        level = 0,
        precision = 6,
        coinId = coin,
    )
}
