package one.mixin.android.db.perps

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.db.BaseDao

@Dao
interface PerpsMarketDao : BaseDao<PerpsMarket> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(market: PerpsMarket)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markets: List<PerpsMarket>)

    @Query("SELECT * FROM markets WHERE CAST(volume AS REAL) > 0 ORDER BY rowid ASC")
    suspend fun getAllMarkets(): List<PerpsMarket>

    @Query("SELECT * FROM markets WHERE CAST(volume AS REAL) > 0 ORDER BY rowid ASC")
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
