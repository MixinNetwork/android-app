package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.db.BaseDao

@Dao
interface PerpsPositionDao : BaseDao<PerpsPosition> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: PerpsPosition)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(positions: List<PerpsPosition>)

    @Query("SELECT * FROM perps_positions WHERE wallet_id = :walletId AND state = 'open' ORDER BY created_at DESC")
    suspend fun getOpenPositions(walletId: String): List<PerpsPosition>

    @Query("SELECT * FROM perps_positions WHERE wallet_id = :walletId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getPositionHistory(walletId: String, limit: Int = 100): List<PerpsPosition>

    @Query("SELECT * FROM perps_positions WHERE position_id = :positionId")
    suspend fun getPosition(positionId: String): PerpsPosition?

    @Query("UPDATE perps_positions SET state = :status, updated_at = :updatedAt WHERE position_id = :positionId")
    suspend fun updateStatus(positionId: String, status: String, updatedAt: String)

    @Query("DELETE FROM perps_positions WHERE wallet_id = :walletId")
    suspend fun deleteByWallet(walletId: String)

    @Query("SELECT SUM(CAST(unrealized_pnl AS REAL)) FROM perps_positions WHERE wallet_id = :walletId AND state = 'open'")
    suspend fun getTotalUnrealizedPnl(walletId: String): Double?
}
