package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsFavorite
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.db.BaseDao

@Dao
interface PerpsFavoriteDao : BaseDao<PerpsFavorite> {
    @Query("SELECT market_id FROM favorites WHERE is_favored = 1 ORDER BY created_at DESC, rowid ASC")
    suspend fun favoriteMarketIds(): List<String>

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(
        marketIds: List<String>,
        createdAt: String,
    ) {
        deleteAll()
        if (marketIds.isNotEmpty()) {
            insertListSuspend(
                marketIds.map { marketId ->
                    PerpsFavorite(
                        marketId = marketId,
                        isFavored = true,
                        createdAt = createdAt,
                    )
                },
            )
        }
    }

    @Query("SELECT market_id FROM favorites WHERE is_favored = 1 ORDER BY created_at DESC, rowid ASC")
    fun observeFavoriteMarketIds(): Flow<List<String>>

    @Query(
        """
        SELECT m.*
        FROM favorites f
        INNER JOIN markets m ON m.market_id = f.market_id
        WHERE f.is_favored = 1
        ORDER BY f.created_at DESC, f.rowid ASC
        """,
    )
    fun observeFavoriteMarkets(): Flow<List<PerpsMarket>>
}
