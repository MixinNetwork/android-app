package one.mixin.android.db.perps

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsMarketCategoryRelation
import one.mixin.android.db.BaseDao

@Dao
interface PerpsMarketCategoryDao : BaseDao<PerpsMarketCategoryRelation> {
    @Query("DELETE FROM market_categories WHERE category = :category")
    suspend fun deleteByCategory(category: Int)

    @Transaction
    suspend fun replaceCategory(
        category: Int,
        marketIds: List<String>,
    ) {
        deleteByCategory(category)
        if (marketIds.isNotEmpty()) {
            insertListSuspend(
                marketIds.map { marketId ->
                    PerpsMarketCategoryRelation(
                        marketId = marketId,
                        category = category,
                    )
                },
            )
        }
    }

    @Query(
        """
        SELECT m.*
        FROM market_categories mc
        INNER JOIN markets m ON m.market_id = mc.market_id
        WHERE mc.category = :category
            AND CAST(m.volume AS REAL) > 0
        ORDER BY mc.rowid ASC
        """,
    )
    fun observeMarketsByCategory(category: Int): Flow<List<PerpsMarket>>
}
