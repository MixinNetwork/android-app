package one.mixin.android.ui.address

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.MixinDatabase
import one.mixin.android.repository.backfillLastUsedFromWithdrawals
import one.mixin.android.repository.missingHistoryTokensQuery
import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeWithdrawal
import one.mixin.android.vo.safe.Token
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.threeten.bp.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class AddressLastUsedTest {
    private lateinit var db: MixinDatabase
    private val chain = Constants.ChainId.ETHEREUM_CHAIN_ID
    private val time = "2026-10-29T00:00:00Z"
    private val address = Address("saved", "address", "asset", chain, "0xAbCd", "Contact", time, null, null)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        db.tokenDao().insertSuspend(token("asset"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun serverSyncKeepsLocalLastUsed() = runBlocking {
        val dao = db.addressDao()
        dao.insertKeepingLastUsed(listOf(address))
        dao.updateLastUsedAt(address.addressId, 100)
        dao.insertKeepingLastUsed(listOf(address.copy(label = "Renamed")))
        val saved = requireNotNull(dao.findAddressById(address.addressId, chain))
        assertEquals("Renamed", saved.label)
        assertEquals(100L, saved.lastUsedAt)
    }

    @Test
    fun markUsedMatchesEvmCaseAndExactTagAndNeverGoesBack() = runBlocking {
        val dao = db.addressDao()
        val tagged = address.copy(addressId = "tagged", tag = "Tag")
        val other = address.copy(addressId = "other", chainId = "other-chain")
        dao.insertKeepingLastUsed(listOf(address, tagged, other))
        dao.markUsed(chain, "0xabcd", "", ignoreCase = true, usedAt = 200)
        dao.markUsed(chain, "0xabcd", "", ignoreCase = true, usedAt = 150)
        dao.markUsed(chain, "0xAbCd", "tag", ignoreCase = true, usedAt = 300)
        assertEquals(200L, dao.findAddressById("saved", chain)?.lastUsedAt)
        assertNull(dao.findAddressById("tagged", chain)?.lastUsedAt)
        assertNull(dao.findAddressById("other", "other-chain")?.lastUsedAt)
        dao.markUsed("other-chain", "0xabcd", "", ignoreCase = false, usedAt = 400)
        assertNull(dao.findAddressById("other", "other-chain")?.lastUsedAt)
    }

    @Test
    fun sendBeforeAddressSyncAppliesWhenAddressArrives() = runBlocking {
        val dao = db.addressDao()
        dao.markUsed(chain, "0xabcd", "", ignoreCase = true, usedAt = 500)
        dao.markUsed(chain, "0xAbCd", "", ignoreCase = true, usedAt = 400)
        dao.markUsed(chain, "0xAbCd", "Tag", ignoreCase = true, usedAt = 900)
        dao.insertKeepingLastUsed(listOf(address, address.copy(addressId = "other", chainId = "other-chain")))
        assertEquals(500L, dao.findAddressById(address.addressId, chain)?.lastUsedAt)
        assertNull(dao.findAddressById("other", "other-chain")?.lastUsedAt)
    }

    @Test
    fun backfillUsesLatestWithdrawalBeforeCutoff() = runBlocking {
        val dao = db.addressDao()
        dao.insertKeepingLastUsed(listOf(address))
        val old = Instant.parse("2026-01-01T00:00:00Z")
        val newer = Instant.parse("2026-02-01T00:00:00Z")
        val afterCutoff = Instant.parse("2026-03-01T00:00:00Z")
        db.safeSnapshotDao().insertListSuspend(
            listOf(withdrawal("1", old), withdrawal("2", newer), withdrawal("3", afterCutoff)),
        )
        backfillLastUsedFromWithdrawals(dao, dao.findAll(), afterCutoff.toEpochMilli())
        assertEquals(newer.toEpochMilli(), dao.findAddressById(address.addressId, chain)?.lastUsedAt)
    }

    @Test
    fun backfillComparesParsedTimestamps() = runBlocking {
        val dao = db.addressDao()
        dao.insertKeepingLastUsed(listOf(address))
        val fraction = "2026-02-01T00:00:00.900123456Z"
        db.safeSnapshotDao().insertListSuspend(
            listOf(withdrawal("1", "2026-02-01T00:00:00Z"), withdrawal("2", fraction)),
        )
        backfillLastUsedFromWithdrawals(dao, dao.findAll(), Instant.parse("2026-03-01T00:00:00Z").toEpochMilli())
        assertEquals(Instant.parse(fraction).toEpochMilli(), dao.findAddressById(address.addressId, chain)?.lastUsedAt)
    }

    @Test
    fun backfillIgnoresWithdrawalsWithoutHash() = runBlocking {
        val dao = db.addressDao()
        dao.insertKeepingLastUsed(listOf(address))
        db.safeSnapshotDao().insertListSuspend(listOf(withdrawal("1", "2026-02-01T00:00:00Z", hash = "")))
        backfillLastUsedFromWithdrawals(dao, dao.findAll(), Instant.parse("2026-03-01T00:00:00Z").toEpochMilli())
        assertNull(dao.findAddressById(address.addressId, chain)?.lastUsedAt)
    }

    @Test
    fun backfillCountsWithdrawalOnceTokenArrives() = runBlocking {
        val dao = db.addressDao()
        dao.insertKeepingLastUsed(listOf(address))
        val cutoff = Instant.parse("2026-03-01T00:00:00Z").toEpochMilli()
        db.safeSnapshotDao().insertListSuspend(listOf(withdrawal("1", "2026-02-01T00:00:00Z", assetId = "late")))
        assertEquals(listOf("late"), dao.queryTexts(missingHistoryTokensQuery(cutoff)))
        backfillLastUsedFromWithdrawals(dao, dao.findAll(), cutoff)
        assertNull(dao.findAddressById(address.addressId, chain)?.lastUsedAt)

        db.tokenDao().insertSuspend(token("late"))
        assertEquals(emptyList(), dao.queryTexts(missingHistoryTokensQuery(cutoff)))
        backfillLastUsedFromWithdrawals(dao, dao.findAll(), cutoff)
        assertEquals(Instant.parse("2026-02-01T00:00:00Z").toEpochMilli(), dao.findAddressById(address.addressId, chain)?.lastUsedAt)
    }

    private fun token(assetId: String) = Token(assetId, assetId, "SYM", assetId, "", "0", "0", chain, "0", "0", 0, "key", "0", null, 18)

    private fun withdrawal(id: String, createdAt: Instant) = withdrawal(id, createdAt.toString())

    private fun withdrawal(id: String, createdAt: String, hash: String = "hash", assetId: String = "asset") = SafeSnapshot(
        id, "withdrawal", assetId, "-1", "user", "", "", "hash", createdAt, null, null, null, null,
        null, SafeWithdrawal(hash, "0xabcd"), null,
    )
}
