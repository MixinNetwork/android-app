package one.mixin.android.db.perps

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.db.BaseDao
import org.threeten.bp.OffsetDateTime

@Dao
interface PerpsPositionDao : BaseDao<PerpsPosition> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: PerpsPosition)

    @Transaction
    suspend fun insertAll(positions: List<PerpsPosition>) {
        positions.forEach { upsertSuspend(it) }
    }

    @Transaction
    override suspend fun upsertSuspend(entity: PerpsPosition) {
        val current = getPosition(entity.positionId)
        // Optimistic opening/adding states can carry a client-generated timestamp.
        if (current != null && current.state != PerpsPosition.STATE_OPENING && current.state != PerpsPosition.STATE_ADDING) {
            val updatedAt = current.updatedAt?.let(OffsetDateTime::parse)
            if (updatedAt != null && OffsetDateTime.parse(entity.updatedAt).isBefore(updatedAt)) return
        }
        insert(entity)
    }

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale 
        FROM positions p 
        LEFT JOIN markets m ON m.market_id = p.market_id 
        WHERE p.wallet_id = :walletId AND (p.state = 'open' or p.state = 'opening' or p.state = 'adding')
        ORDER BY p.created_at DESC
    """)
    suspend fun getOpenPositions(walletId: String): List<PerpsPositionItem>

    @Query(
        """
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.market_id
        WHERE p.wallet_id = :walletId AND (p.state = 'open' or p.state = 'opening' or p.state = 'adding')
        ORDER BY p.created_at DESC
    """
    )
    fun observeOpenPositions(walletId: String): Flow<List<PerpsPositionItem>>

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.market_id
        WHERE p.wallet_id = :walletId AND (p.state = 'open' or p.state = 'opening' or p.state = 'adding')
        ORDER BY p.created_at DESC
    """)
    fun getOpenPositionsPaged(walletId: String): PagingSource<Int, PerpsPositionItem>

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale 
        FROM positions p 
        LEFT JOIN markets m ON m.market_id = p.market_id 
        WHERE p.position_id = :positionId
    """)
    suspend fun getPosition(positionId: String): PerpsPositionItem?

    @Query("""
        SELECT p.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM positions p
        LEFT JOIN markets m ON m.market_id = p.market_id
        WHERE p.position_id = :positionId
    """)
    fun observePosition(positionId: String): Flow<PerpsPositionItem?>

    @Query("UPDATE positions SET state = :status, updated_at = :updatedAt WHERE position_id = :positionId")
    suspend fun updateStatus(positionId: String, status: String, updatedAt: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId")
    suspend fun deleteByWallet(walletId: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening' or state = 'adding')")
    suspend fun deleteOpenByWallet(walletId: String)

    @Query("DELETE FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening' or state = 'adding') AND position_id NOT IN (:positionIds)")
    suspend fun deleteOpenByWalletAndNotIn(walletId: String, positionIds: List<String>)

    @Query("SELECT COALESCE(SUM(CAST(unrealized_pnl AS REAL)), 0) FROM positions WHERE wallet_id = :walletId AND (state = 'open' OR state = 'opening' OR state = 'adding')")
    suspend fun getTotalUnrealizedPnl(walletId: String): Double?

    @Query("SELECT COALESCE(SUM(CAST(unrealized_pnl AS REAL)), 0) FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening' or state = 'adding')")
    fun observeTotalUnrealizedPnl(walletId: String): Flow<Double>

    @Query("SELECT SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))) FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening' or state = 'adding')")
    suspend fun getTotalOpenPositionValue(walletId: String): Double?

    @Query("SELECT COALESCE(SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))), 0) FROM positions WHERE wallet_id = :walletId AND (state = 'open' or state = 'opening' or state = 'adding')")
    fun observeTotalOpenPositionValue(walletId: String): Flow<Double>

    @Query("DELETE FROM positions WHERE position_id = :positionId")
    fun deleteById(positionId: String)
}
