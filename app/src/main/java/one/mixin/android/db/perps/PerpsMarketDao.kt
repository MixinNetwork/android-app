package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.db.BaseDao

@Dao
interface PerpsMarketDao : BaseDao<PerpsMarket> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(market: PerpsMarket)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markets: List<PerpsMarket>)

    @Query("SELECT * FROM markets WHERE market_id IN (:marketIds)")
    suspend fun getMarkets(marketIds: List<String>): List<PerpsMarket>

    @Transaction
    suspend fun upsertPreservingTradeVolumeScores(markets: List<PerpsMarket>): List<PerpsMarket> {
        if (markets.isEmpty()) return emptyList()
        val existingScores =
            getMarkets(markets.map(PerpsMarket::marketId))
                .associate { market -> market.marketId to market.tradeVolumeScore1D }
        val mergedMarkets =
            markets.map { market ->
                market.copy(
                    tradeVolumeScore1D = existingScores[market.marketId] ?: 0,
                )
            }
        upsertList(mergedMarkets)
        return mergedMarkets
    }

    @Query(
        """
        SELECT *
        FROM markets
        WHERE CAST(volume AS REAL) > 0
        ORDER BY trade_volume_score_1d DESC, CAST(volume AS REAL) DESC,
            token_symbol COLLATE NOCASE ASC, market_id ASC
        """,
    )
    suspend fun getAllMarkets(): List<PerpsMarket>

    @Query(
        """
        SELECT *
        FROM markets
        WHERE CAST(volume AS REAL) > 0
        ORDER BY trade_volume_score_1d DESC, CAST(volume AS REAL) DESC,
            token_symbol COLLATE NOCASE ASC, market_id ASC
        """,
    )
    fun observeAllMarkets(): Flow<List<PerpsMarket>>

    @Query("SELECT * FROM markets WHERE market_id = :marketId")
    suspend fun getMarket(marketId: String): PerpsMarket?

    @Query(
        """
        SELECT * FROM markets
        WHERE CAST(volume AS REAL) > 0
            AND (
                display_symbol LIKE '%' || :query || '%'
            OR token_symbol LIKE '%' || :query || '%'
            OR quote_symbol LIKE '%' || :query || '%'
            )
        ORDER BY CAST(volume AS REAL) DESC
        """
    )
    suspend fun searchMarkets(query: String): List<PerpsMarket>

    @Query("DELETE FROM markets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM markets")
    suspend fun getCount(): Int
}
