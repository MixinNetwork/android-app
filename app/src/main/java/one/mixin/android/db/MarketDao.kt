package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem

@Dao
interface MarketDao : BaseDao<Market> {
    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id LEFT JOIN market_coins mc ON mc.coin_id = m.coin_id WHERE mc.asset_id = :assetId")
    fun marketById(assetId: String): LiveData<MarketItem?>

    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id WHERE m.coin_id = :coinId")
    fun marketByCoinId(coinId: String): LiveData<MarketItem?>

    @Query(
        """
       SELECT * FROM (
            SELECT m.*, mf.is_favored 
            FROM markets m 
            LEFT JOIN market_favored mf ON mf.coin_id = m.coin_id
            ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, CAST(m.market_cap AS INTEGER)
            LIMIT :limit
        ) AS limitedMarkets
        ORDER BY 
            CASE WHEN :sortValue = 0 THEN CAST(limitedMarkets.market_cap_rank AS INTEGER) END ASC,
            CASE WHEN :sortValue = 1 THEN CAST(limitedMarkets.market_cap_rank AS INTEGER) END DESC,
            CASE WHEN :sortValue = 2 THEN CAST(limitedMarkets.current_price AS DECIMAL) END ASC,
            CASE WHEN :sortValue = 3 THEN CAST(limitedMarkets.current_price AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 4 THEN CAST(limitedMarkets.price_change_percentage_7d AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 5 THEN CAST(limitedMarkets.price_change_percentage_7d AS DECIMAL) END ASC
        """
    )
    fun getWeb3Markets(limit: Int, sortValue: Int): PagingSource<Int, MarketItem>

    @Query(
        """
         SELECT * FROM (
            SELECT m.*, mf.is_favored 
            FROM markets m 
            LEFT JOIN market_favored mf ON mf.coin_id = m.coin_id 
            WHERE mf.is_favored = 1 
            ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, CAST(m.market_cap AS INTEGER)
        ) AS limitedFavoredMarkets
        ORDER BY 
            CASE WHEN :sortValue = 0 THEN CAST(limitedFavoredMarkets.market_cap_rank AS INTEGER) END ASC,
            CASE WHEN :sortValue = 1 THEN CAST(limitedFavoredMarkets.market_cap_rank AS INTEGER) END DESC,
            CASE WHEN :sortValue = 2 THEN CAST(limitedFavoredMarkets.current_price AS DECIMAL) END ASC,
            CASE WHEN :sortValue = 3 THEN CAST(limitedFavoredMarkets.current_price AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 4 THEN CAST(limitedFavoredMarkets.price_change_percentage_7d AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 5 THEN CAST(limitedFavoredMarkets.price_change_percentage_7d AS DECIMAL) END ASC
        """
    )
    fun getFavoredWeb3Markets(sortValue: Int): PagingSource<Int, MarketItem>

    @Query("SELECT * FROM markets WHERE coin_id = :coinId")
    fun findMarketById(coinId: String): Market?

    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id LEFT JOIN market_coins mc ON mc.coin_id = m.coin_id WHERE mc.asset_id = :assetId")
    suspend fun findMarketItemByAssetId(assetId: String): MarketItem?

    @Query("DELETE FROM markets WHERE CAST(market_cap_rank AS INTEGER) <= :top")
    fun deleteTop(top: Int): Int
}