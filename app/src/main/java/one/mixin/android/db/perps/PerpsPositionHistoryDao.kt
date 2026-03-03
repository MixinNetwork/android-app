package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.DataSource
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
        FROM position_history h 
        LEFT JOIN markets m ON m.market_id = h.product_id 
        WHERE h.wallet_id = :walletId 
        ORDER BY h.closed_at DESC 
        LIMIT :limit
    """)
    suspend fun getHistories(walletId: String, limit: Int): List<PerpsPositionHistoryItem>

    @Query("""
        SELECT h.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM position_history h
        LEFT JOIN markets m ON m.market_id = h.product_id
        WHERE h.wallet_id = :walletId
        ORDER BY h.closed_at DESC
    """)
    fun getHistoriesPaged(walletId: String): DataSource.Factory<Int, PerpsPositionHistoryItem>

    @Query("""
        SELECT h.*, m.display_symbol, m.icon_url, m.token_symbol 
        FROM position_history h 
        LEFT JOIN markets m ON m.market_id = h.product_id 
        WHERE h.history_id = :historyId
    """)
    suspend fun getHistory(historyId: String): PerpsPositionHistoryItem?

    @Query("DELETE FROM position_history WHERE wallet_id = :walletId")
    suspend fun deleteByWallet(walletId: String)

    @Query("SELECT SUM(CAST(realized_pnl AS REAL)) FROM position_history WHERE wallet_id = :walletId")
    suspend fun getTotalRealizedPnl(walletId: String): Double?

    @Query("SELECT SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))) FROM position_history WHERE wallet_id = :walletId")
    suspend fun getTotalClosedEntryValue(walletId: String): Double?
}
