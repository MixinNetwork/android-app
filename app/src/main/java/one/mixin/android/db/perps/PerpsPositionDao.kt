package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.DataSource
import kotlinx.coroutines.flow.Flow
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
        WHERE p.wallet_id = :walletId AND (p.state = 'open' or p.state = 'opening')
        ORDER BY p.created_at DESC
    """)
    suspend fun getOpenPositions(walletId: String): List<PerpsPositionItem>

    @Query(
        """
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.product_id
        WHERE p.wallet_id = :walletId AND (p.state = 'open' or p.state = 'opening')
        ORDER BY p.created_at DESC
    """
    )
    fun observeOpenPositions(walletId: String): Flow<List<PerpsPositionItem>>

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.product_id
        WHERE p.wallet_id = :walletId AND (p.state = 'open' or p.state = 'opening')
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

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.product_id
        WHERE p.position_id = :positionId
    """)
    fun observePosition(positionId: String): Flow<PerpsPositionItem?>

    @Query("UPDATE positions SET state = :status, updated_at = :updatedAt WHERE position_id = :positionId")
    suspend fun updateStatus(positionId: String, status: String, updatedAt: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId")
    suspend fun deleteByWallet(walletId: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening')")
    suspend fun deleteOpenByWallet(walletId: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening') AND position_id NOT IN (:positionIds)")
    suspend fun deleteOpenByWalletAndNotIn(walletId: String, positionIds: List<String>)

    @Query("SELECT SUM(CAST(unrealized_pnl AS REAL)) FROM positions WHERE wallet_id = :walletId AND state = (state = 'open' or state = 'opening')")
    suspend fun getTotalUnrealizedPnl(walletId: String): Double?

    @Query("SELECT COALESCE(SUM(CAST(unrealized_pnl AS REAL)), 0) FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening')")
    fun observeTotalUnrealizedPnl(walletId: String): Flow<Double>

    @Query("SELECT SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))) FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening')")
    suspend fun getTotalOpenPositionValue(walletId: String): Double?

    @Query("SELECT COALESCE(SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))), 0) FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening')")
    fun observeTotalOpenPositionValue(walletId: String): Flow<Double>

    @Query("DELETE FROM positions WHERE position_id = :positionId")
    fun deleteById(positionId: String)
}
