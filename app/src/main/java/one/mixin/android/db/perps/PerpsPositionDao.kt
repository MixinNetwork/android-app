package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.DataSource
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.db.BaseDao

@Dao
interface PerpsPositionDao : BaseDao<PerpsPosition> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: PerpsPosition)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(positions: List<PerpsPosition>)

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol 
        FROM positions p 
        LEFT JOIN markets m ON m.market_id = p.product_id 
        WHERE p.wallet_id = :walletId AND p.state = 'open' 
        ORDER BY p.created_at DESC
    """)
    suspend fun getOpenPositions(walletId: String): List<PerpsPositionItem>

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.product_id
        WHERE p.wallet_id = :walletId AND p.state = 'open'
        ORDER BY p.created_at DESC
    """)
    fun getOpenPositionsPaged(walletId: String): DataSource.Factory<Int, PerpsPositionItem>

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol 
        FROM positions p 
        LEFT JOIN markets m ON m.market_id = p.product_id 
        WHERE p.position_id = :positionId
    """)
    suspend fun getPosition(positionId: String): PerpsPositionItem?

    @Query("UPDATE positions SET state = :status, updated_at = :updatedAt WHERE position_id = :positionId")
    suspend fun updateStatus(positionId: String, status: String, updatedAt: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId")
    suspend fun deleteByWallet(walletId: String)

    @Query("SELECT SUM(CAST(unrealized_pnl AS REAL)) FROM positions WHERE wallet_id = :walletId AND state = 'open'")
    suspend fun getTotalUnrealizedPnl(walletId: String): Double?
}
