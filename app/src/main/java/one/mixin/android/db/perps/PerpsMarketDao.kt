package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.db.BaseDao

@Dao
interface PerpsMarketDao : BaseDao<PerpsMarket> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(market: PerpsMarket)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markets: List<PerpsMarket>)

    @Query("SELECT * FROM perps_markets ORDER BY CAST(volume AS REAL) DESC")
    suspend fun getAllMarkets(): List<PerpsMarket>

    @Query("SELECT * FROM perps_markets WHERE market_id = :marketId")
    suspend fun getMarket(marketId: String): PerpsMarket?

    @Query("SELECT * FROM perps_markets WHERE symbol LIKE '%' || :query || '%' ORDER BY CAST(volume AS REAL) DESC")
    suspend fun searchMarkets(query: String): List<PerpsMarket>

    @Query("DELETE FROM perps_markets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM perps_markets")
    suspend fun getCount(): Int
}
