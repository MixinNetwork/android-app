package one.mixin.android.ui.address

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.wallet.FilterParams
import one.mixin.android.ui.wallet.SnapshotType
import one.mixin.android.ui.wallet.WalletFilterQueryGenerated
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.vo.Address
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.SafeDeposit
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeWithdrawal
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.toAssetItem
import one.mixin.android.vo.safe.toWeb3TokenItem
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.threeten.bp.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class AddressTransactionsTest {
    private lateinit var main: MixinDatabase
    private lateinit var wallet: WalletDatabase
    private val chain = Constants.ChainId.ETHEREUM_CHAIN_ID
    private val time = "2026-10-29T00:00:00Z"
    private val address = Address("saved", "address", "asset", chain, "0xAbCd", "Contact", time, null, null)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        main = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        wallet = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        main.tokenDao().insertSuspend(token("asset"), token("other-asset"), token("wrong-chain", "other-chain"))
        addWallet("wallet-a", "OWNER-A")
    }

    @After
    fun tearDown() {
        main.close()
        wallet.close()
    }

    @Test
    fun privacyMatchesWholeDestinationAndExactTagAcrossAssets() = runBlocking {
        main.safeSnapshotDao().insertListSuspend(listOf(
            snapshot("01", "0xabcd"), snapshot("02", "0xabcd:Tag"),
            snapshot("03", "0xAbCd:tag"), snapshot("04", "0xAbCd-more"),
            snapshot("05", "0xAbCd", "other-asset"), snapshot("06", "0xAbCd", "wrong-chain"),
            snapshot("07", "unused").copy(deposit = SafeDeposit("hash", "0xABCD"), withdrawal = null),
            snapshot("08", "unused").copy(deposit = SafeDeposit("hash", "0xABCD:Tag"), withdrawal = null),
            snapshot("09", "0xabcd:Tag-more"),
            snapshot("10", "miss").copy(withdrawal = SafeWithdrawal("0xabcd:Tag", "miss")),
        ))
        assertEquals(listOf("07", "05", "01"), privacy().map { it.snapshotId })
        assertEquals(listOf("08", "02"), privacy(FilterParams(address = address.copy(tag = "Tag"))).map { it.snapshotId })
        assertEquals(listOf("03"), privacy(FilterParams(address = address.copy(tag = "tag"))).map { it.snapshotId })
        assertEquals(listOf("07", "05", "01"), privacy(FilterParams(address = address.copy(tag = ""))).map { it.snapshotId })
        assertTrue(privacy(FilterParams(address = address.copy(destination = "0xABC"))).isEmpty())
    }

    @Test
    fun validEvmDestinationsIgnoreAsciiCaseWithoutFoldingTags() = runBlocking {
        val destination = "0x" + "aB".repeat(20)
        val filter = address.copy(destination = destination, tag = "Tag")
        main.safeSnapshotDao().insertSuspend(
            snapshot("exact", "${destination.lowercase()}:Tag"),
            snapshot("wrong-tag", "${destination.lowercase()}:tag"),
            snapshot("wrong-address", "${destination.dropLast(1)}c:Tag"),
        )
        assertEquals(listOf("exact"), privacy(FilterParams(address = filter)).map { it.snapshotId })
        wallet.web3TransactionDao().insertSuspend(transaction("exact").copy(senders = listOf(AssetChange("asset", "1", to = destination.lowercase()))))
        assertEquals(listOf("exact"), web3(Web3FilterParams(walletId = "wallet-a", address = filter.copy(tag = null))).map { it.transactionHash })
        assertTrue(web3(Web3FilterParams(walletId = "wallet-a", address = filter)).isEmpty())
    }

    @Test
    fun web3MatchesEveryEndpointButNotAccountMetadataOrMissingTag() = runBlocking {
        val hit = AssetChange("asset", "1", from = "0xaBcD", to = "other")
        val miss = AssetChange("asset", "1", from = "other", to = "different")
        val matches = listOf("senders-from", "senders-to", "receivers-from", "receivers-to", "approvals-from", "approvals-to")
        wallet.web3TransactionDao().insertListSuspend(matches.map { id ->
            val changes = listOf(miss, if (id.endsWith("from")) hit else hit.copy(from = "other", to = "0xABCD"))
            transaction(id).copy(
                senders = if (id.startsWith("senders")) changes else emptyList(),
                receivers = if (id.startsWith("receivers")) changes else emptyList(),
                approvals = if (id.startsWith("approvals")) changes else null,
            )
        } + listOf(
            transaction("no-endpoints").copy(senders = emptyList()),
            transaction("metadata").copy(senders = listOf(AssetChange("0xabcd", "1", to = "miss"))),
            transaction("prefix").copy(senders = listOf(hit.copy(from = "0xabcd-more"))),
            transaction("other-chain").copy(chainId = "other-chain"),
        ))
        assertEquals(matches.sortedDescending(), web3().map { it.transactionHash })
        assertTrue(web3(Web3FilterParams(walletId = "wallet-a", address = address.copy(tag = "123"))).isEmpty())
        assertEquals(matches.sortedDescending(), web3(Web3FilterParams(walletId = "wallet-a", address = address.copy(tag = ""))).map { it.transactionHash })
        assertTrue(web3(Web3FilterParams(walletId = "wallet-a", address = address.copy(destination = "owner-a"))).isEmpty())
    }

    @Test
    fun currentWalletIncludesEveryAccountWithoutDuplicatesOrCrossChainOwnership() = runBlocking {
        addWallet("wallet-b", "OWNER-B")
        wallet.web3AddressDao().insertSuspend(
            Web3Address("duplicate", "wallet-a", chain, "owner-a", null, time),
            Web3Address("second", "wallet-a", chain, "OWNER-C", null, time),
            Web3Address("wrong-network", "wallet-a", "other-chain", "elsewhere", null, time),
        )
        wallet.web3TokenDao().insertSuspend(
            Web3Token("wallet-a", "asset", chain, "A", "key", "A", "", 18, balance = "0", priceUsd = "0", changeUsd = "0"),
            Web3Token("wallet-b", "asset", chain, "B", "key", "B", "", 18, balance = "0", priceUsd = "0", changeUsd = "0"),
        )
        wallet.web3TransactionDao().insertSuspend(
            transaction("same-hash"), transaction("same-hash").copy(address = "owner-c"),
            transaction("same-hash").copy(address = "owner-b"), transaction("unowned").copy(address = "elsewhere"),
            transaction("wrong-chain").copy(chainId = "other-chain"),
        )
        val first = web3()
        assertEquals(listOf("owner-c", "owner-a"), first.map { it.address })
        assertEquals(listOf("A", "A"), first.map { it.sendAssetSymbol })
        val second = web3(Web3FilterParams(walletId = "wallet-b", address = address))
        assertEquals(listOf("owner-b"), second.map { it.address })
        assertEquals(listOf("B"), second.map { it.sendAssetSymbol })
    }

    @Test
    fun privacyAndWeb3RemainSeparateEvenWithEqualIdentifiers() = runBlocking {
        main.safeSnapshotDao().insertSuspend(snapshot("same-hash", "0xabcd"), snapshot("privacy-only", "0xabcd"))
        wallet.web3TransactionDao().insertSuspend(transaction("same-hash"), transaction("web3-only"))
        assertEquals(listOf("same-hash", "privacy-only"), privacy().map { it.snapshotId })
        assertEquals(listOf("web3-only", "same-hash"), web3().map { it.transactionHash })
    }

    @Test
    fun databaseLimitsAndPreviewAgreeWithPagingForZeroTenElevenAndMoreMatches() = runBlocking {
        for (count in listOf(0, 10, 11, 15)) {
            val filter = address.copy(destination = "0x$count")
            main.safeSnapshotDao().insertListSuspend((0 until count).map { index ->
                snapshot("$count-${index.toString().padStart(2, '0')}", filter.destination)
            })
            wallet.web3TransactionDao().insertListSuspend((0 until count).map { index ->
                transaction("$count-${index.toString().padStart(2, '0')}").copy(senders = listOf(AssetChange("asset", "1", to = filter.destination)))
            })
            val privacyParams = FilterParams(address = filter)
            val web3Params = Web3FilterParams(walletId = "wallet-a", address = filter)
            assertTrue(privacyParams.buildQuery(11).sql.trimEnd().endsWith("LIMIT 11"))
            assertTrue(web3Params.buildQuery(11).sql.trimEnd().endsWith("LIMIT 11"))
            assertEquals(count, privacy(privacyParams).size)
            assertEquals(count, web3(web3Params).size)
            for (limit in listOf(0, 10, 11)) {
                assertEquals(minOf(count, limit), privacy(privacyParams, limit).size)
                assertEquals(minOf(count, limit), web3(web3Params, limit).size)
            }
        }
    }

    @Test
    fun sparseMatchesKeepStableOrderingAcrossTimesAndAccounts() = runBlocking {
        wallet.web3AddressDao().insertSuspend(Web3Address("second", "wallet-a", chain, "OWNER-C", null, time))
        val snapshots = (0..114).map { index ->
            snapshot(index.toString().padStart(3, '0'), if (index < 15) "0xabcd" else "miss")
                .copy(createdAt = if (index % 2 == 0) time else "2026-10-29T01:00:00Z")
        }
        main.safeSnapshotDao().insertListSuspend(snapshots)
        val transactions = snapshots.map { item ->
            transaction(item.snapshotId).copy(transactionAt = item.createdAt, senders = listOf(AssetChange("asset", "1", to = item.withdrawal!!.receiver)))
        }
        wallet.web3TransactionDao().insertListSuspend(transactions + transactions.take(5).map { it.copy(address = "owner-c") })
        for (order in listOf(SortOrder.Recent, SortOrder.Oldest)) {
            val privacyRows = privacy(FilterParams(address = address, order = order))
            val web3Rows = web3(Web3FilterParams(walletId = "wallet-a", address = address, order = order))
            val expectedPrivacy = snapshots.take(15).sortedWith(compareBy<SafeSnapshot> { it.createdAt }.thenBy { it.snapshotId })
            val expectedWeb3 = (transactions.take(15) + transactions.take(5).map { it.copy(address = "owner-c") })
                .sortedWith(compareBy<Web3Transaction> { it.transactionAt }.thenBy { it.transactionHash }.thenBy { it.address })
            assertEquals((if (order == SortOrder.Recent) expectedPrivacy.reversed() else expectedPrivacy).map { it.snapshotId }, privacyRows.map { it.snapshotId })
            assertEquals((if (order == SortOrder.Recent) expectedWeb3.reversed() else expectedWeb3).map { it.transactionHash to it.address }, web3Rows.map { it.transactionHash to it.address })
            privacy(FilterParams(address = address, order = order), 11)
            web3(Web3FilterParams(walletId = "wallet-a", address = address, order = order), 11)
        }
    }

    @Test
    fun jsonEscapingAndSqlEscapingDoNotTurnAddressesOrTagsIntoPatterns() = runBlocking {
        val destinations = listOf("0x%_\"\\<>&='\n\t\u0000", "%_", "' OR 1=1 --")
        val tags = listOf("Tag", "tag", "标签", "%_\"\\'\n\t")
        for ((index, destination) in destinations.withIndex()) {
            main.safeSnapshotDao().insertSuspend(
                snapshot("withdrawal-$index", destination),
                snapshot("deposit-$index", "unused").copy(deposit = SafeDeposit("hash", destination), withdrawal = null),
                snapshot("prefix-$index", "$destination-more"),
                snapshot("metadata-$index", "miss").copy(withdrawal = SafeWithdrawal(destination, "miss")),
            )
            for ((tagIndex, tag) in tags.withIndex()) {
                main.safeSnapshotDao().insertSuspend(snapshot("tag-$index-$tagIndex", "$destination:$tag"))
            }
            val hit = AssetChange("asset", "1", from = "other", to = destination)
            wallet.web3TransactionDao().insertSuspend(
                transaction("senders-$index").copy(senders = listOf(hit)),
                transaction("receivers-$index").copy(senders = emptyList(), receivers = listOf(hit.copy(from = destination, to = "other"))),
                transaction("approvals-$index").copy(senders = emptyList(), approvals = listOf(hit)),
                transaction("prefix-$index").copy(senders = listOf(hit.copy(to = "$destination-more"))),
                transaction("metadata-$index").copy(senders = listOf(AssetChange(destination, "1", to = "miss", symbol = "\"to\":\"$destination\""))),
            )
            val filter = address.copy(destination = destination)
            assertEquals(setOf("withdrawal-$index", "deposit-$index"), privacy(FilterParams(address = filter)).map { it.snapshotId }.toSet())
            assertEquals(setOf("senders-$index", "receivers-$index", "approvals-$index"), web3(Web3FilterParams(walletId = "wallet-a", address = filter)).map { it.transactionHash }.toSet())
            for ((tagIndex, tag) in tags.withIndex()) {
                assertEquals(listOf("tag-$index-$tagIndex"), privacy(FilterParams(address = filter.copy(tag = tag))).map { it.snapshotId })
            }
        }
        assertTrue(privacy(FilterParams(address = address.copy(chainId = "' OR 1=1 --"))).isEmpty())
        assertTrue(web3(Web3FilterParams(walletId = "wallet-a", address = address.copy(chainId = "' OR 1=1 --"))).isEmpty())
        assertTrue(web3(Web3FilterParams(walletId = "' OR 1=1 --", address = address)).isEmpty())
        val walletId = "wallet'quoted"
        addWallet(walletId, "quoted-owner")
        wallet.web3TokenDao().insertSuspend(Web3Token(walletId, "asset", chain, "Q", "key", "Q", "", 18, balance = "0", priceUsd = "0", changeUsd = "0"))
        wallet.web3TransactionDao().insertSuspend(transaction("quoted").copy(address = "quoted-owner"))
        assertEquals("Q", web3(Web3FilterParams(walletId = walletId, address = address)).single().sendAssetSymbol)
    }

    @Test
    fun nonEvmDestinationsAndWalletAccountsAreCaseSensitive() = runBlocking {
        val solana = Constants.ChainId.Solana
        main.tokenDao().insertSuspend(token("solana", solana))
        wallet.web3AddressDao().insertSuspend(Web3Address("solana", "wallet-a", solana, "Owner", null, time))
        main.safeSnapshotDao().insertSuspend(snapshot("upper", "Destination", "solana"), snapshot("lower", "destination", "solana"))
        wallet.web3TransactionDao().insertSuspend(
            transaction("upper").copy(chainId = solana, address = "Owner", senders = listOf(AssetChange("solana", "1", to = "Destination"))),
            transaction("lower").copy(chainId = solana, address = "Owner", senders = listOf(AssetChange("solana", "1", to = "destination"))),
            transaction("wrong-account-case").copy(chainId = solana, address = "owner", senders = listOf(AssetChange("solana", "1", to = "Destination"))),
        )
        for ((destination, id) in listOf("Destination" to "upper", "destination" to "lower")) {
            val filter = address.copy(chainId = solana, destination = destination)
            assertEquals(listOf(id), privacy(FilterParams(address = filter)).map { it.snapshotId })
            assertEquals(listOf(id), web3(Web3FilterParams(walletId = "wallet-a", address = filter)).map { it.transactionHash })
        }
    }

    @Test
    fun addressFiltersPreserveDefaultGoodLevelAndExplicitReputationFilters() = runBlocking {
        wallet.web3TransactionDao().insertListSuspend(listOf(0, 1, 10, 11, 12).map { transaction("level-$it").copy(level = it) })
        assertEquals(listOf(12, 11), web3().map { it.level })
        for ((level, expected) in listOf(
            Web3FilterParams.FILTER_GOOD_AND_UNKNOWN to listOf(12, 11, 10),
            Web3FilterParams.FILTER_GOOD_AND_SPAM to listOf(12, 11, 1, 0),
            Web3FilterParams.FILTER_ALL to listOf(12, 11, 10, 1, 0),
        )) {
            assertEquals(expected, web3(Web3FilterParams(walletId = "wallet-a", address = address, level = level)).map { it.level })
        }
    }

    @Test
    fun addressFiltersComposeWithTokenTypeAndDateFilters() = runBlocking {
        val deposit = snapshot("kept", "unused").copy(deposit = SafeDeposit("hash", "0xABCD"), withdrawal = null)
        main.safeSnapshotDao().insertSuspend(
            deposit, deposit.copy(snapshotId = "wrong-token", assetId = "other-asset"),
            deposit.copy(snapshotId = "old", createdAt = "2026-10-28T00:00:00Z"),
            deposit.copy(snapshotId = "new", createdAt = "2026-10-31T00:00:00Z"), snapshot("withdrawal", "0xabcd"),
        )
        val day = Instant.parse(time).toEpochMilli()
        val privacyParams = FilterParams(address = address, type = SnapshotType.deposit, tokenItems = listOf(token("asset").toAssetItem()), startTime = day, endTime = day)
        assertEquals(listOf("kept"), privacy(privacyParams).map { it.snapshotId })
        wallet.web3TransactionDao().insertSuspend(
            transaction("kept"), transaction("wrong-token").copy(sendAssetId = "other-asset"),
            transaction("old").copy(transactionAt = "2026-10-28T00:00:00Z"),
            transaction("new").copy(transactionAt = "2026-10-31T00:00:00Z"),
            transaction("receive").copy(transactionType = "transfer_in"), transaction("unknown").copy(level = 10),
        )
        val web3Params = Web3FilterParams(walletId = "wallet-a", address = address, tokenItems = listOf(token("asset").toAssetItem().toWeb3TokenItem("wallet-a")), startTime = day, endTime = day)
        web3Params.tokenFilterType = enumValueOf("SEND")
        assertEquals(listOf("kept"), web3(web3Params).map { it.transactionHash })
    }

    @Test
    fun absentAddressAndLimitKeepOriginalQueries() {
        assertEquals(WalletFilterQueryGenerated.snapshots("", "ORDER BY s.created_at DESC").sql, FilterParams().buildQuery().sql)
        assertEquals(
            WalletFilterQueryGenerated.web3Transactions("WHERE w.address IN (SELECT destination FROM addresses WHERE wallet_id = 'wallet-a') AND w.level >= 11", "ORDER BY w.transaction_at DESC", "wallet-a").sql,
            Web3FilterParams(walletId = "wallet-a").buildQuery().sql,
        )
    }

    private suspend fun privacy(params: FilterParams = FilterParams(address = address), limit: Int? = null): List<SnapshotItem> {
        val dao = main.safeSnapshotDao()
        val preview = dao.snapshotItems(params.buildQuery(limit))
        val paged = dao.getSnapshots(params.buildQuery()).items()
        assertEquals(paged.take(limit ?: Int.MAX_VALUE).map { it.snapshotId }, preview.map { it.snapshotId })
        return preview
    }

    private suspend fun web3(params: Web3FilterParams = Web3FilterParams(walletId = "wallet-a", address = address), limit: Int? = null): List<Web3TransactionItem> {
        val dao = wallet.web3TransactionDao()
        val preview = dao.transactionItems(params.buildQuery(limit))
        val paged = dao.allTransactions(params.buildQuery()).items()
        assertEquals(paged.take(limit ?: Int.MAX_VALUE), preview)
        return preview
    }

    private suspend fun <T : Any> PagingSource<Int, T>.items(): List<T> {
        var page = assertIs<PagingSource.LoadResult.Page<Int, T>>(load(PagingSource.LoadParams.Refresh(null, 7, false)))
        val items = page.data.toMutableList()
        while (page.nextKey != null) {
            page = assertIs(load(PagingSource.LoadParams.Append(checkNotNull(page.nextKey), 7, false)))
            items += page.data
        }
        return items
    }

    private suspend fun addWallet(id: String, account: String) {
        wallet.web3WalletDao().insertSuspend(Web3Wallet(id, "watch_address", id, time, time))
        wallet.web3AddressDao().insertSuspend(Web3Address(id, id, chain, account, null, time))
    }

    private fun token(id: String, chainId: String = chain) = Token(id, id, "SYM", id, "", "0", "0", chainId, "0", "0", 0, "key", "0", null, 18)

    private fun snapshot(id: String, receiver: String, assetId: String = "asset") = SafeSnapshot(
        id, "snapshot", assetId, "1", "user", "", "", "hash", time, null, null, null, null,
        null, SafeWithdrawal("hash", receiver), null,
    )

    private fun transaction(hash: String) = Web3Transaction(
        transactionHash = hash, chainId = chain, address = "owner-a", transactionType = "transfer_out", status = "success",
        blockNumber = 1, fee = "0", senders = listOf(AssetChange("asset", "1", "other", "0xabcd")),
        receivers = emptyList(), sendAssetId = "asset", transactionAt = time, createdAt = time, updatedAt = time,
        level = Constants.AssetLevel.GOOD,
    )
}
