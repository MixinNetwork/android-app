package one.mixin.android.db

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Looper
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.response.web3.groupSwapTokens
import one.mixin.android.db.web3.syncMarketCoins
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.groupWeb3Tokens
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.web3.details.Web3TransactionsFragment
import one.mixin.android.web3.receive.Web3TokenAdapter
import one.mixin.android.web3.swap.SwapTokenAdapter
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketCoin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class Web3TokenGroupingTest {
    private lateinit var main: MixinDatabase
    private lateinit var wallet: WalletDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = object : Resources(context.assets, context.resources.displayMetrics, context.resources.configuration) {
            override fun getStringArray(id: Int): Array<String> = when (id) {
                R.array.currency_names -> arrayOf("USD")
                R.array.currency_symbols -> arrayOf("$")
                else -> super.getStringArray(id)
            }
        }
        MixinApplication.appContext = object : ContextWrapper(context) {
            override fun getResources() = resources
        }
        main = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        wallet = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
    }

    @After
    fun tearDown() {
        main.close()
        wallet.close()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun mirrorBackfillsUpdatesDeletesAndRecoversWithoutChangingBalances() = runBlocking {
        val coins = listOf(MarketCoin("ethereum", "usd-coin", "1"), MarketCoin("base", "usd-coin", "1"))
        main.marketCoinDao().insertListSuspend(coins)
        wallet.web3MarketCoinDao().insertSuspend(MarketCoin("stale", "old", "0"))
        wallet.web3TokenDao().insertSuspend(token("ethereum", "12.3456"))
        val scope = CoroutineScope(Dispatchers.IO)
        var sync = scope.syncMarketCoins(main.marketCoinDao(), wallet)
        suspend fun awaitCoins(expected: List<MarketCoin>) = withTimeout(5_000) {
            while (wallet.web3MarketCoinDao().findByAssetIds(listOf("ethereum", "base", "stale")).toSet() != expected.toSet()) delay(10)
        }
        try {
            awaitCoins(coins)
            val updated = listOf(coins.first().copy(coinId = "new-coin", createdAt = "2"))
            main.withRoomTransaction {
                main.marketCoinDao().deleteByCoinIds(listOf("usd-coin"))
                main.marketCoinDao().insertListSuspend(updated)
            }
            awaitCoins(updated)
            sync.cancelAndJoin()
            main.marketCoinDao().deleteByCoinIds(listOf("new-coin"))
            assertEquals(updated, wallet.web3MarketCoinDao().findByAssetIds(listOf("ethereum")))
            sync = scope.syncMarketCoins(main.marketCoinDao(), wallet)
            awaitCoins(emptyList())
            assertEquals("12.3456", wallet.web3TokenDao().findWeb3TokenItems("wallet").single().balance)
        } finally {
            sync.cancelAndJoin()
        }
    }

    @Test
    fun totalsKeepWalletsAndUnmappedAssetsSeparateAndRetainSpendableTokens() = runBlocking {
        wallet.web3TokenDao().insertListSuspend(listOf(token("ethereum", "0.1"), token("base", "0.2", price = "2")))
        wallet.web3MarketCoinDao().insertListSuspend(listOf(MarketCoin("ethereum", "usd-coin", ""), MarketCoin("base", "usd-coin", "")))
        val tokens = wallet.web3TokenDao().findWeb3TokenItems("wallet")
        val ethereum = tokens.first { it.assetId == "ethereum" }
        val groups = (tokens + ethereum + ethereum.copy(walletId = "other-wallet") +
            ethereum.copy(assetId = "unknown-1", coinId = null) + ethereum.copy(assetId = "unknown-2", coinId = " ")).groupWeb3Tokens()
        assertEquals(4, groups.size)
        val group = groups.first()
        assertEquals(0, BigDecimal("0.3").compareTo(group.balance))
        assertEquals(0, BigDecimal("0.5").multiply(Fiats.getRate().toBigDecimal()).compareTo(group.fiat))
        assertEquals("0.1", ethereum.balance)
        assertEquals("0.1", ethereum.toSwapToken().balance)
        assertEquals("usd-coin", ethereum.toSwapToken().coinId)
        assertEquals("usd-coin", ethereum.toTokenItem().coinId)
        assertEquals(1, tokens.map { it.toSwapToken() }.groupSwapTokens().size)
        val send = Web3TokenAdapter(grouped = true).apply { this.tokens = ArrayList(tokens) }
        assertEquals(1, send.itemCount)
        assertEquals(tokens.toSet(), send.groupAssets(ethereum.copy(coinId = null, balance = "999")).toSet())
        send.chain = "base"
        assertEquals(listOf("base"), send.groupAssets(ethereum).map { it.assetId })
        val receive = Web3TokenAdapter().apply { this.tokens = ArrayList(tokens) }
        assertEquals(2, receive.itemCount)
        val swap = SwapTokenAdapter(grouped = true).apply { this.tokens = tokens.map { it.toSwapToken() } }
        assertEquals(1, swap.itemCount)
        assertEquals(setOf("0.1", "0.2"), swap.groupAssets(ethereum.toSwapToken()).map { it.balance }.toSet())
    }

    @Test
    fun queriesCountGroupsAndKeepHiddenBalancesAndWalletsSeparate() = runBlocking {
        wallet.web3TokenDao().insertListSuspend(listOf(
            token("ethereum", "1"), token("base", "2"), token("hidden", "5"),
            token("unmapped", "3"), token("ethereum", "100", walletId = "other-wallet"),
        ))
        wallet.web3MarketCoinDao().insertListSuspend(listOf("ethereum", "base", "hidden").map { MarketCoin(it, "usd-coin", "") })
        wallet.web3TokensExtraDao().insertSuspend(Web3TokensExtra("wallet", "hidden", true))
        val dao = wallet.web3TokenDao()
        val summary = awaitValue(dao.walletHomeWeb3TokenSummary("wallet"))
        assertEquals(2, summary.tokenCount)
        assertEquals(1, summary.hiddenTokenCount)
        assertEquals(6.0, summary.totalUsd)
        assertEquals(setOf("ethereum", "base", "hidden"), dao.findGroupTokenIds("wallet", "base").toSet())
        assertEquals(listOf("ethereum"), dao.findGroupTokenIds("other-wallet", "base"))
        assertEquals(setOf("ethereum", "base", "unmapped"), awaitValue(dao.walletHomeWeb3TokenPreview("wallet")).map { it.assetId }.toSet())
        assertTrue(awaitValue(dao.groupedTokenItems("wallet", "base")).all { it.walletId == "wallet" })
        wallet.web3TokenDao().insertListSuspend(listOf(token("blank-1", "0"), token("blank-2", "0")))
        wallet.web3MarketCoinDao().insertListSuspend(listOf("blank-1", "blank-2").map { MarketCoin(it, " ", "") })
        assertEquals(listOf("blank-1"), dao.findGroupTokenIds("wallet", "blank-1"))
        assertEquals(4, awaitValue(dao.walletHomeWeb3TokenSummary("wallet")).tokenCount)
    }

    @Test
    fun transactionQueriesRespectSelectedNetworkAndWallet() = runBlocking {
        wallet.web3TokenDao().insertListSuspend(listOf(token("ethereum", "1"), token("base", "2")))
        wallet.web3AddressDao().insertSuspend(Web3Address("address", "wallet", "ethereum", "owner", null, ""))
        listOf("ethereum", "base").forEach { id ->
            wallet.web3TransactionDao().insertSuspend(transaction(id, "owner"), transaction(id, "other-owner"))
        }
        val dao = wallet.web3TransactionDao()
        assertEquals(setOf("ethereum", "base"), awaitValue(dao.web3Transactions("wallet", listOf("ethereum", "base"))).map { it.chainId }.toSet())
        assertEquals(listOf("base"), awaitValue(dao.web3Transactions("wallet", listOf("base"))).map { it.chainId })
        val selected = wallet.web3TokenDao().findWeb3TokenItems("wallet").filter { it.assetId == "base" }
        val detail = Web3TransactionsFragment.newInstance("owner", selected.single(), "base").requireArguments()
        assertEquals("base", detail.getString(Web3TransactionsFragment.ARGS_NETWORK))
        assertEquals("owner", detail.getString(Web3TransactionsFragment.ARGS_ADDRESS))
        assertEquals(null, Web3TransactionsFragment.newInstance("owner", selected.single()).requireArguments().getString(Web3TransactionsFragment.ARGS_NETWORK))
        val page = dao.allTransactions(Web3FilterParams(walletId = "wallet", tokenItems = selected).buildQuery()).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )
        assertTrue(page is PagingSource.LoadResult.Page)
        assertEquals(listOf("base"), page.data.map { it.chainId })
        assertEquals(listOf("owner"), page.data.map { it.address })
    }

    private fun token(id: String, balance: String, price: String = "1", walletId: String = "wallet") = Web3Token(
        walletId, id, id, "USD Coin", id, "USDC", "", 6, "", balance, price, "0",
    )

    private fun transaction(id: String, owner: String) = Web3Transaction(
        transactionHash = "$id-$owner", chainId = id, address = owner, transactionType = "transfer_in",
        status = "success", blockNumber = 1, fee = "0", senders = emptyList(), receivers = emptyList(),
        receiveAssetId = id, transactionAt = "2026-09-08T00:00:00Z", createdAt = "", updatedAt = "", level = 100,
    )

    private suspend fun <T : Any> awaitValue(data: LiveData<T>): T {
        val values = LinkedBlockingQueue<T>()
        val observer = Observer<T> { values.offer(it) }
        data.observeForever(observer)
        return try {
            withTimeout(5_000) {
                var value: T? = null
                while (value == null) {
                    shadowOf(Looper.getMainLooper()).idle()
                    value = values.poll()
                    if (value == null) delay(10)
                }
                value
            }
        } finally {
            data.removeObserver(observer)
        }
    }
}
