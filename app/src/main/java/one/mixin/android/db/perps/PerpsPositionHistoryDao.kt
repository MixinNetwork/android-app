package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.DataSource
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsPositionHistory
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.db.BaseDao

@Dao
interface PerpsPositionHistoryDao : BaseDao<PerpsPositionHistory> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PerpsPositionHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<PerpsPositionHistory>)

    @Query("""
        SELECT h.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM position_histories h 
        LEFT JOIN markets m ON m.market_id = h.market_id 
        WHERE (:offset IS NULL OR h.closed_at < :offset)
        ORDER BY h.closed_at DESC 
        LIMIT :limit
    """)
    suspend fun getHistories(limit: Int, offset: String? = null): List<PerpsPositionHistoryItem>

    @Query(
        """
        SELECT h.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM position_histories h
        LEFT JOIN markets m ON m.market_id = h.market_id
        WHERE (:offset IS NULL OR h.closed_at < :offset)
        ORDER BY h.closed_at DESC
        LIMIT :limit
    """
    )
    fun observeHistories(limit: Int, offset: String? = null): Flow<List<PerpsPositionHistoryItem>>

    @Query("""
        SELECT h.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM position_histories h
        LEFT JOIN markets m ON m.market_id = h.market_id
        ORDER BY h.closed_at DESC
    """)
    fun getHistoriesPaged(): DataSource.Factory<Int, PerpsPositionHistoryItem>

    @Query("""
        SELECT h.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM position_histories h 
        LEFT JOIN markets m ON m.market_id = h.market_id 
        WHERE h.history_id = :historyId
    """)
    suspend fun getHistory(historyId: String): PerpsPositionHistoryItem?

    @Query("DELETE FROM position_histories")
    suspend fun deleteAll()

    @Query("SELECT MAX(closed_at) FROM position_histories")
    suspend fun getLatestClosedAt(): String?

    @Query("SELECT SUM(CAST(realized_pnl AS REAL)) FROM position_histories")
    suspend fun getTotalRealizedPnl(): Double?

    @Query("SELECT COALESCE(SUM(CAST(realized_pnl AS REAL)), 0) FROM position_histories")
    fun observeTotalRealizedPnl(): Flow<Double>

    @Query("SELECT SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))) FROM position_histories")
    suspend fun getTotalClosedEntryValue(): Double?

    @Query("SELECT COALESCE(SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))), 0) FROM position_histories")
    fun observeTotalClosedEntryValue(): Flow<Double>
}
