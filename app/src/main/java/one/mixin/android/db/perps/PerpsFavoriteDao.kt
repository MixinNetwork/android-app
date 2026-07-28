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
    @Query("SELECT market_id FROM favorites WHERE is_favored = 1")
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

    @Query("SELECT market_id FROM favorites WHERE is_favored = 1")
    fun observeFavoriteMarketIds(): Flow<List<String>>

    @Query(
        """
        SELECT m.*
        FROM favorites f
        INNER JOIN markets m ON m.market_id = f.market_id
        LEFT JOIN ranks r ON r.market_id = m.market_id
        WHERE f.is_favored = 1
        ORDER BY CASE WHEN r.`rank` IS NULL THEN 1 ELSE 0 END, r.`rank` ASC, f.created_at DESC
        """,
    )
    fun observeFavoriteMarkets(): Flow<List<PerpsMarket>>
}
