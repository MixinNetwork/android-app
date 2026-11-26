package one.mixin.android.ui.address

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.MixinDatabase
import one.mixin.android.repository.backfillLastUsedFromWithdrawals
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
        db.tokenDao().insertSuspend(Token("asset", "asset", "SYM", "asset", "", "0", "0", chain, "0", "0", 0, "key", "0", null, 18))
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
    fun backfillUsesLatestWithdrawalBeforeCutoff() = runBlocking {
        val dao = db.addressDao()
        dao.insertKeepingLastUsed(listOf(address))
        val old = Instant.parse("2026-01-01T00:00:00Z")
        val newer = Instant.parse("2026-02-01T00:00:00Z")
        val afterCutoff = Instant.parse("2026-03-01T00:00:00Z")
        db.safeSnapshotDao().insertListSuspend(
            listOf(withdrawal("1", old), withdrawal("2", newer), withdrawal("3", afterCutoff)),
        )
        backfillLastUsedFromWithdrawals(dao, db.safeSnapshotDao(), dao.findAll(), afterCutoff.toEpochMilli())
        assertEquals(newer.toEpochMilli(), dao.findAddressById(address.addressId, chain)?.lastUsedAt)
    }

    private fun withdrawal(id: String, createdAt: Instant) = SafeSnapshot(
        id, "withdrawal", "asset", "-1", "user", "", "", "hash", createdAt.toString(), null, null, null, null,
        null, SafeWithdrawal("hash", "0xabcd"), null,
    )
}
