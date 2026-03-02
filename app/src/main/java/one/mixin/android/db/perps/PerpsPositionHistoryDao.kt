package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
        WHERE h.history_id = :historyId
    """)
    suspend fun getHistory(historyId: String): PerpsPositionHistoryItem?

    @Query("DELETE FROM position_history WHERE wallet_id = :walletId")
    suspend fun deleteByWallet(walletId: String)
}
