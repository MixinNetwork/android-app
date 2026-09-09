package one.mixin.android.db

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.ui.wallet.FilterParams
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokensExtra
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TokenGroupingDaoTest {
    @Test
    fun coinBackfillIncludesZeroBalanceAssetsAndExcludesMappedAssetsAndInscriptions() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), MixinDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            listOf("held", "hidden", "zero", "no-extra", "mapped", "inscription").forEach { id ->
                database.tokenDao().insertSuspend(
                    Token(id, id, "USDC", "USD Coin", "", "0", "1", id, "0", "0", 0, id, "0", if (id == "inscription") "collection" else null, 6),
                )
                if (id != "no-extra") {
                    database.tokensExtraDao().insertSuspend(
                        TokensExtra(id, id, id == "hidden", if (id == "held") "1" else "0", ""),
                    )
                }
            }
            database.marketCoinDao().insertSuspend(MarketCoin("mapped", "usd-coin", ""))
            assertEquals(
                setOf("held", "hidden", "zero", "no-extra"),
                database.marketCoinDao().findAssetsWithoutCoin().toSet(),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun transactionFilterIncludesTheGroupAndRespectsExplicitNetworkAssets() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), MixinDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            listOf("ethereum", "base", "unrelated").forEachIndexed { index, id ->
                database.tokenDao().insertSuspend(
                    Token(id, id, "USDC", "USD Coin", "", "0", "1", id, "0", "0", 0, id, "0", null, 6),
                )
                database.marketCoinDao().insertSuspend(
                    MarketCoin(id, if (id == "unrelated") "other" else "usd-coin", ""),
                )
                database.safeSnapshotDao().insertSuspend(
                    SafeSnapshot(
                        snapshotId = id,
                        type = "snapshot",
                        assetId = id,
                        amount = "1",
                        userId = "user",
                        opponentId = "opponent",
                        memo = "",
                        transactionHash = "",
                        createdAt = "2026-09-07T00:00:0${index}Z",
                        traceId = null,
                        confirmations = null,
                        openingBalance = null,
                        closingBalance = null,
                        deposit = null,
                        withdrawal = null,
                        inscriptionHash = null,
                    ),
                )
            }
            val token = requireNotNull(database.tokenDao().simpleAssetItem("ethereum"))
            assertEquals("usd-coin", token.coinId)
            assertEquals(
                setOf("ethereum", "base"),
                database.tokenDao().findGroupedAssetItems(listOf("ethereum")).map { it.assetId }.toSet(),
            )
            val filter = FilterParams(tokenItems = listOf(token))
            suspend fun selectedAssets(): Set<String> {
                val page = database.safeSnapshotDao().getSnapshots(filter.buildQuery()).load(
                    PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
                )
                check(page is PagingSource.LoadResult.Page) { "$page" }
                return page.data.map { it.assetId }.toSet()
            }
            assertEquals(setOf("ethereum", "base"), selectedAssets())
            filter.assetIds = listOf("base")
            assertEquals(setOf("base"), selectedAssets())
            filter.assetIds = null
            filter.tokenItems = listOf(requireNotNull(database.tokenDao().simpleAssetItem("unrelated")))
            assertEquals(setOf("unrelated"), selectedAssets())
        } finally {
            database.close()
        }
    }
}
