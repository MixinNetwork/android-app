package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.vo.market.MarketCategoryRelation
import one.mixin.android.vo.market.MarketItem

@Dao
interface MarketCategoryDao : BaseDao<MarketCategoryRelation> {
    @Query("DELETE FROM market_categories WHERE category = :category")
    suspend fun deleteByCategory(category: Int)

    @Query("DELETE FROM market_categories WHERE coin_id = :coinId AND category = :category")
    suspend fun deleteByCoinIdAndCategory(
        coinId: String,
        category: Int,
    )

    @Transaction
    suspend fun replaceCategory(
        category: Int,
        coinIds: List<String>,
    ) {
        deleteByCategory(category)
        if (coinIds.isNotEmpty()) {
            insertListSuspend(
                coinIds.map { coinId ->
                    MarketCategoryRelation(
                        coinId = coinId,
                        category = category,
                    )
                },
            )
        }
    }

    @Query(
        """
        SELECT m.*, mf.is_favored
        FROM market_categories mc
        INNER JOIN markets m ON m.coin_id = mc.coin_id
        LEFT JOIN market_favored mf ON mf.coin_id = m.coin_id
        LEFT JOIN market_cap_ranks mr ON mr.coin_id = m.coin_id
        WHERE mc.category = :category
        ORDER BY CASE WHEN mr.market_cap_rank IS NULL THEN 1 ELSE 0 END,
            CAST(mr.market_cap_rank AS INTEGER) ASC
        """,
    )
    fun observeMarketsByCategory(category: Int): Flow<List<MarketItem>>

    @Query(
        """
        SELECT m.*, mf.is_favored
        FROM market_categories mc
        INNER JOIN markets m ON m.coin_id = mc.coin_id
        LEFT JOIN market_favored mf ON mf.coin_id = m.coin_id
        LEFT JOIN market_cap_ranks mr ON mr.coin_id = m.coin_id
        WHERE mc.category = :category
        ORDER BY CASE WHEN mr.market_cap_rank IS NULL THEN 1 ELSE 0 END,
            CAST(mr.market_cap_rank AS INTEGER) ASC
        """,
    )
    suspend fun marketsByCategory(category: Int): List<MarketItem>
}
