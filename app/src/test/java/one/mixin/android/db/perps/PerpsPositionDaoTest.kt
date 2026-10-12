package one.mixin.android.db.perps

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.db.PerpsDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerpsPositionDaoTest {
    private lateinit var database: PerpsDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PerpsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun olderDetailAndListSnapshotsDoNotRollBackMargin() = runBlocking {
        val dao = database.perpsPositionDao()
        val initial = position()
        val increased = initial.copy(margin = "55", updatedAt = "2026-10-07T00:00:00.250Z")
        dao.upsertSuspend(initial)
        assertEquals("5", dao.getOpenPositions("wallet").single().margin)

        dao.insertAll(listOf(increased))
        dao.upsertSuspend(initial)
        assertEquals("55", dao.getPosition(initial.positionId)?.margin)
        dao.insertAll(listOf(initial))
        assertEquals("55", dao.getOpenPositions("wallet").single().margin)

        val reduced = increased.copy(margin = "50", updatedAt = "2026-10-07T00:00:01Z")
        dao.upsertSuspend(reduced)
        dao.insertAll(listOf(increased))
        assertEquals("50", dao.getPosition(initial.positionId)?.margin)
    }

    @Test
    fun equalVersionSnapshotsStillRefreshQuotes() = runBlocking {
        val dao = database.perpsPositionDao()
        val initial = position()
        dao.upsertSuspend(initial)
        val refreshed = initial.copy(markPrice = "101", unrealizedPnl = "1", updatedAt = "2026-10-07T08:00:00+08:00")
        dao.insertAll(listOf(refreshed))
        assertEquals("101", dao.getPosition(initial.positionId)?.markPrice)
        assertEquals("1", dao.getPosition(initial.positionId)?.unrealizedPnl)
    }

    @Test
    fun remoteSnapshotsReplaceOptimisticLocalStates() = runBlocking {
        val dao = database.perpsPositionDao()
        val remote = position()
        for (state in listOf(PerpsPosition.STATE_OPENING, PerpsPosition.STATE_ADDING)) {
            dao.insert(remote.copy(state = state, updatedAt = "2026-10-07T08:00:00Z"))
            dao.upsertSuspend(remote)
            assertEquals(PerpsPosition.STATE_OPEN, dao.getPosition(remote.positionId)?.state)
            assertEquals(remote.updatedAt, dao.getPosition(remote.positionId)?.updatedAt)
        }
    }

    private fun position() = PerpsPosition(
        positionId = "position",
        marketId = "market",
        side = "long",
        quantity = "1",
        entryPrice = "100",
        margin = "5",
        leverage = 10,
        state = PerpsPosition.STATE_OPEN,
        markPrice = "100",
        unrealizedPnl = "0",
        roe = "0",
        settleAssetId = "asset",
        openPayAmount = "5",
        openPayAssetId = "asset",
        botId = "bot",
        walletId = "wallet",
        createdAt = "2026-10-07T00:00:00Z",
        updatedAt = "2026-10-07T00:00:00Z",
    )
}
