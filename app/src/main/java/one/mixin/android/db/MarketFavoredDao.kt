package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import one.mixin.android.vo.market.MarketFavored

@Dao
interface MarketFavoredDao : BaseDao<MarketFavored> {
    @Query("SELECT coin_id FROM market_favored WHERE is_favored = 1")
    suspend fun favoriteMarketIds(): List<String>

    @Query("DELETE FROM market_favored")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(markets: List<MarketFavored>) {
        deleteAll()
        if (markets.isNotEmpty()) {
            insertListSuspend(markets)
        }
    }
}
