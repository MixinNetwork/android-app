package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.market.MarketFavored

@Dao
interface MarketFavoredDao : BaseDao<MarketFavored> {
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
