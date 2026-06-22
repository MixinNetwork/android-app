package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.MarketFavored

@Dao
interface MarketFavoredDao : BaseDao<MarketFavored> {
    @Query("DELETE FROM market_favored WHERE coin_id = :coinId")
    suspend fun deleteByCoinId(coinId: String)
}
