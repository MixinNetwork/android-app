package one.mixin.android.repository

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.birbit.android.jobqueue.config.Configuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.withRoomTransaction
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.job.MixinJobManager
import org.bitcoinj.core.Transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class TokenRepositoryWalletTransactionTest {
    private lateinit var main: MixinDatabase
    private lateinit var wallet: WalletDatabase
    private lateinit var jobs: MixinJobManager
    private lateinit var context: Context
    private lateinit var repository: TokenRepository
    private val chain = Constants.ChainId.Solana
    private val createdAt = "2026-09-08T00:00:00Z"
    private val updatedAt = "2026-09-08T00:01:00Z"

    @Before
    fun setUp() = runBlocking<Unit> {
        context = ApplicationProvider.getApplicationContext()
        main = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        wallet = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        jobs = MixinJobManager(Configuration.Builder(context).inTestMode()
            .minConsumerCount(0).maxConsumerCount(1).networkUtil { 0 }.build())
        repository = repository()
        RoomDatabaseCompat.execute(main, "INSERT INTO raw_transactions (request_id, raw_transaction, receiver_id, created_at, state, type) VALUES ('main-sentinel', 'preserve', 'receiver', '', 'unspent', 0)")
        wallet.web3AddressDao().insertSuspend(Web3Address("address", "wallet", chain, "sender", null, createdAt))
    }

    @After
    fun tearDown() = runBlocking<Unit> {
        try {
            RoomDatabaseCompat.query(main, "SELECT request_id, raw_transaction FROM raw_transactions").use {
                assertTrue(it.moveToFirst())
                assertEquals("main-sentinel", it.getString(0))
                assertEquals("preserve", it.getString(1))
                assertEquals(1, it.count)
            }
            for (database in listOf(main, wallet)) {
                RoomDatabaseCompat.query(database, "PRAGMA integrity_check").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("ok", it.getString(0))
                }
            }
        } finally {
            main.close()
            wallet.close()
            withContext(Dispatchers.IO) { jobs.destroy() }
        }
    }

    @Test
    fun mainTransactionCannotRollBackWalletWrites() = runBlocking<Unit> {
        assertFails {
            main.withRoomTransaction {
                wallet.web3RawTransactionDao().insertSuspend(rawRecord("outside-wallet-transaction"))
                error("abort main transaction")
            }
        }
        assertNotNull(raw("outside-wallet-transaction"))
    }

    @Test
    fun signedPendingInsertUsesWalletAndPreservesAmounts() = runBlocking<Unit> {
        insertSigned()
        val raw = assertNotNull(raw("signed"))
        assertEquals("signed-raw", raw.raw)
        assertEquals("pending", raw.state)
        assertEquals("sender", raw.account)
        val transaction = assertNotNull(transaction("signed"))
        assertEquals("-10.00", transaction.senders!!.single().amount)
        assertEquals("10.00", transaction.receivers!!.single().amount)
        assertEquals("fee-asset", transaction.sponsorFeeAssetId)
        assertEquals("0.01", transaction.sponsorFeeAmount)
    }

    @Test
    fun sponsorPendingInsertIsIdempotent() = runBlocking<Unit> {
        repeat(2) {
            repository.insertGaslessPendingTransaction("sponsor", chain, "sender", "asset", "10", "fee-asset", "0.0100", "receiver", "7", createdAt, updatedAt)
        }
        assertEquals("gasless:sponsor:sponsor", assertNotNull(raw("sponsor")).raw)
        assertEquals("7", assertNotNull(raw("sponsor")).nonce)
        assertEquals(1, rowCount("raw_transactions"))
        assertEquals(1, rowCount("transactions"))
    }

    @Test
    fun failedInsertRollsBackBothRowsAndReleasesWriter() = runBlocking<Unit> {
        failOn("INSERT", "transactions")
        assertFails { insertSigned() }
        assertNull(raw("signed"))
        assertNull(transaction("signed"))
        clearFailure()
        withTimeout(5_000) { insertSigned() }
    }

    @Test
    fun cancelledInsertRollsBackRawRowAndReleasesWriter() = runBlocking<Unit> {
        val cancelled = repository(object : Web3TransactionDao by wallet.web3TransactionDao() {
            override fun insert(vararg obj: Web3Transaction) {
                throw CancellationException("cancel transaction insert")
            }
        })
        val error = assertFails { insertSigned(cancelled) }
        assertTrue(error is CancellationException)
        assertNull(raw("signed"))
        assertNull(transaction("signed"))
        withTimeout(5_000) { insertSigned() }
    }

    @Test
    fun replacementMovesBothRowsAndPreservesFee() = runBlocking<Unit> {
        insertSigned()
        repository.replaceGaslessPendingTransactionHash("wallet", "signed", "broadcast", chain, updatedAt)
        assertNull(raw("signed"))
        assertNull(transaction("signed"))
        assertEquals("gasless:broadcast:broadcast", assertNotNull(raw("broadcast")).raw)
        assertEquals("0.01", assertNotNull(transaction("broadcast")).sponsorFeeAmount)
        assertEquals(updatedAt, assertNotNull(transaction("broadcast")).updatedAt)
    }

    @Test
    fun replacementWithoutTransactionMovesOnlyRawRow() = runBlocking<Unit> {
        wallet.web3RawTransactionDao().insertSuspend(rawRecord("signed"))
        repository.replaceGaslessPendingTransactionHash("wallet", "signed", "broadcast", chain, updatedAt)
        assertNull(raw("signed"))
        assertNotNull(raw("broadcast"))
        assertEquals(0, rowCount("transactions"))
    }

    @Test
    fun failedReplacementRestoresOldHashAndRemovesNewHash() = runBlocking<Unit> {
        insertSigned()
        failOn("DELETE", "raw_transactions")
        assertFails { repository.replaceGaslessPendingTransactionHash("wallet", "signed", "broadcast", chain, updatedAt) }
        assertNotNull(raw("signed"))
        assertNotNull(transaction("signed"))
        assertNull(raw("broadcast"))
        assertNull(transaction("broadcast"))
    }

    @Test
    fun missingHashWrongWalletAndWrongChainAreNoOps() = runBlocking<Unit> {
        insertSigned()
        for ((walletId, hash, chainId) in listOf(Triple("wallet", "missing", chain), Triple("other", "signed", chain), Triple("wallet", "signed", "other-chain"))) {
            repository.replaceGaslessPendingTransactionHash(walletId, hash, "broadcast", chainId, updatedAt)
            repository.updateGaslessPendingTransactionStatus(walletId, hash, chainId, "failed", updatedAt)
        }
        assertEquals("pending", assertNotNull(raw("signed")).state)
        assertEquals("pending", assertNotNull(transaction("signed")).status)
        assertNull(raw("broadcast"))
        withTimeout(5_000) { wallet.withRoomTransaction { Unit } }
    }

    @Test
    fun statusUpdatesKeepRawAndTransactionConsistent() = runBlocking<Unit> {
        insertSigned()
        for (status in listOf("success", "failed", "notfound", "pending")) {
            repository.updateGaslessPendingTransactionStatus("wallet", "signed", chain, status, updatedAt)
            assertEquals(status, assertNotNull(raw("signed")).state)
            assertEquals(status, assertNotNull(transaction("signed")).status)
            assertEquals(updatedAt, assertNotNull(raw("signed")).updatedAt)
        }
    }

    @Test
    fun statusUpdateWithoutTransactionStillUpdatesRawRow() = runBlocking<Unit> {
        wallet.web3RawTransactionDao().insertSuspend(rawRecord("signed"))
        repository.updateGaslessPendingTransactionStatus("wallet", "signed", chain, "failed", updatedAt)
        assertEquals("failed", assertNotNull(raw("signed")).state)
        assertEquals(0, rowCount("transactions"))
    }

    @Test
    fun failedStatusUpdateRollsBackRawState() = runBlocking<Unit> {
        insertSigned()
        failOn("UPDATE", "transactions")
        assertFails { repository.updateGaslessPendingTransactionStatus("wallet", "signed", chain, "failed", updatedAt) }
        assertEquals("pending", assertNotNull(raw("signed")).state)
        assertEquals("pending", assertNotNull(transaction("signed")).status)
    }

    @Test
    fun rawInsertAndStatusUpdatePersistWithoutUtxoCleanup() = runBlocking<Unit> {
        insertSigned()
        repository.insertRawTransactionAndUpdateTransactionStatus(rawRecord("signed").copy(state = "success"), "signed", "success", chain, null)
        assertEquals("success", assertNotNull(raw("signed")).state)
        assertEquals("success", assertNotNull(transaction("signed")).status)
    }

    @Test
    fun rawInsertAndStatusUpdateRollBackTogether() = runBlocking<Unit> {
        insertSigned()
        failOn("UPDATE", "transactions")
        assertFails { repository.insertRawTransactionAndUpdateTransactionStatus(rawRecord("signed").copy(state = "success"), "signed", "success", chain, null) }
        assertEquals("pending", assertNotNull(raw("signed")).state)
        assertEquals("pending", assertNotNull(transaction("signed")).status)
    }

    @Test
    fun blankAndMalformedUtxoHexDoNotDeleteOutputsOrHoldWriter() = runBlocking<Unit> {
        val bitcoin = Constants.ChainId.BITCOIN_CHAIN_ID
        val output = output("keep", "11".repeat(32), bitcoin)
        wallet.walletOutputDao().insertSuspend(output)
        for (hex in listOf(null, "", "  ", "0x", "not-hex", "00")) {
            repository.insertRawTransactionAndUpdateTransactionStatus(rawRecord("bitcoin", bitcoin), "bitcoin", "failed", bitcoin, hex)
            assertEquals(output, wallet.walletOutputDao().outputByOutpoint(output.transactionHash, 0, bitcoin))
        }
        withTimeout(5_000) { wallet.withRoomTransaction { Unit } }
    }

    @Test
    fun utxoCleanupDeletesOnlyUnprotectedOutputs() = runBlocking<Unit> {
        val bitcoin = Constants.ChainId.BITCOIN_CHAIN_ID
        val previousHash = "11".repeat(32)
        val hex = "0100000001${previousHash}0000000000ffffffff0101000000000000000000000000"
        val tx = Transaction.read(ByteBuffer.wrap(hex.hexStringToByteArray()))
        val ownHash = tx.txId.toString()
        val signedInput = output("input", previousHash, bitcoin, "signed")
        val pendingOutput = output("pending", ownHash, bitcoin, "pending")
        val unrelated = output("unrelated", "22".repeat(32), bitcoin)
        wallet.walletOutputDao().insertSuspend(signedInput, pendingOutput, unrelated)
        repository.insertRawTransactionAndUpdateTransactionStatus(rawRecord(ownHash, bitcoin).copy(raw = hex, state = "failed"), ownHash, "failed", bitcoin, "0x$hex")
        assertNull(wallet.walletOutputDao().outputByOutpoint(previousHash, 0, bitcoin))
        assertNull(wallet.walletOutputDao().outputByOutpoint(ownHash, 0, bitcoin))
        assertEquals(unrelated, wallet.walletOutputDao().outputByOutpoint(unrelated.transactionHash, 0, bitcoin))
    }

    @Test
    fun utxoCleanupPreservesInputReferencedByAnotherPendingTransaction() = runBlocking<Unit> {
        val bitcoin = Constants.ChainId.BITCOIN_CHAIN_ID
        val previousHash = "11".repeat(32)
        val hex = "0100000001${previousHash}0000000000ffffffff0101000000000000000000000000"
        val signedInput = output("input", previousHash, bitcoin, "signed")
        wallet.walletOutputDao().insertSuspend(signedInput)
        wallet.web3RawTransactionDao().insertSuspend(rawRecord("other-pending", bitcoin).copy(raw = hex))
        repository.insertRawTransactionAndUpdateTransactionStatus(rawRecord("failed", bitcoin).copy(raw = hex, state = "failed"), "failed", "failed", bitcoin, hex)
        assertEquals(signedInput, wallet.walletOutputDao().outputByOutpoint(previousHash, 0, bitcoin))
    }

    private suspend fun insertSigned(repo: TokenRepository = repository) {
        repo.insertGaslessSignedPendingTransaction("signed", chain, "sender", "asset", "-10.00", "fee-asset", "0.0100", "receiver", "signed-raw", createdAt, updatedAt)
    }

    private fun rawRecord(hash: String, chainId: String = chain) = Web3RawTransaction(hash, chainId, "sender", "", "raw", "pending", createdAt, createdAt)
    private suspend fun raw(hash: String) = wallet.web3RawTransactionDao().getRawTransactionByHashAndChain("wallet", hash, chain)
    private suspend fun transaction(hash: String) = wallet.web3TransactionDao().getLatestTransaction(hash, chain)
    private fun output(id: String, hash: String, chainId: String, status: String = "unspent") = WalletOutput(id, chainId, hash, 0, "1", "sender", "", "", status, createdAt, createdAt)

    private fun rowCount(table: String): Int = RoomDatabaseCompat.query(wallet, "SELECT COUNT(*) FROM $table").use {
        assertTrue(it.moveToFirst())
        it.getInt(0)
    }

    private fun failOn(operation: String, table: String) {
        RoomDatabaseCompat.execute(wallet, "CREATE TRIGGER fail_write BEFORE $operation ON $table BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
    }

    private fun clearFailure() = RoomDatabaseCompat.execute(wallet, "DROP TRIGGER fail_write")

    private fun repository(transactions: Web3TransactionDao = wallet.web3TransactionDao()) = TokenRepository(
        appDatabase = main,
        walletDatabase = wallet,
        tokenService = unavailableService(),
        assetService = unavailableService(),
        utxoService = unavailableService(),
        userService = unavailableService(),
        routeService = unavailableService(),
        tokenDao = main.tokenDao(),
        tokensExtraDao = main.tokensExtraDao(),
        safeSnapshotDao = main.safeSnapshotDao(),
        addressDao = main.addressDao(),
        addressService = unavailableService(),
        hotAssetDao = main.topAssetDao(),
        traceDao = main.traceDao(),
        chainDao = main.chainDao(),
        depositDao = main.depositDao(),
        rawTransactionDao = main.rawTransactionDao(),
        outputDao = main.outputDao(),
        userDao = main.userDao(),
        inscriptionDao = main.inscriptionDao(),
        inscriptionCollectionDao = main.inscriptionCollectionDao(),
        historyPriceDao = main.historyPriceDao(),
        marketDao = main.marketDao(),
        marketCoinDao = main.marketCoinDao(),
        marketFavoredDao = main.marketFavoredDao(),
        marketCapRankDao = main.marketCapRankDao(),
        marketCategoryDao = main.marketCategoryDao(),
        alertDao = main.alertDao(),
        orderDao = wallet.orderDao(),
        web3TokenDao = wallet.web3TokenDao(),
        web3TransactionDao = transactions,
        web3RawTransactionDao = wallet.web3RawTransactionDao(),
        walletOutputDao = wallet.walletOutputDao(),
        web3WalletDao = wallet.web3WalletDao(),
        jobManager = jobs,
        safeBoxStoreManager = SafeBoxStoreManager(context),
        web3AddressDao = wallet.web3AddressDao(),
    )

    private inline fun <reified T> unavailableService(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected network call: ${method.name}")
    } as T
}
