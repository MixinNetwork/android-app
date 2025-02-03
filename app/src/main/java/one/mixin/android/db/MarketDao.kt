
package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import one.mixin.android.ui.wallet.alert.vo.CoinItem
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem

@Dao
interface MarketDao : BaseDao<Market> {
    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id LEFT JOIN market_coins mc ON mc.coin_id = m.coin_id WHERE mc.asset_id = :assetId")
    fun marketById(assetId: String): LiveData<MarketItem?>

    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id WHERE m.coin_id = :coinId")
    fun marketByCoinId(coinId: String): LiveData<MarketItem?>

    @Query(
        "SELECT * FROM markets WHERE symbol LIKE '%' || :query || '%'  ESCAPE '\\' OR name LIKE '%' || :query || '%'  ESCAPE '\\'"
    )
    fun fuzzyMarkets(query: String):List<Market>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
       SELECT * FROM (
            SELECT m.*, mf.is_favored 
            FROM market_cap_ranks mr    
            LEFT JOIN markets m ON m.coin_id = mr.coin_id
            LEFT JOIN market_favored mf ON mf.coin_id = m.coin_id
            WHERE m.coin_id IS NOT NULL
            ORDER BY CAST(mr.market_cap_rank AS INTEGER) ASC
            LIMIT :limit
        ) AS limitedMarkets
        ORDER BY 
            CASE WHEN :sortValue = 0 THEN CAST(limitedMarkets.market_cap_rank AS INTEGER) END ASC,
            CASE WHEN :sortValue = 1 THEN CAST(limitedMarkets.market_cap_rank AS INTEGER) END DESC,
            CASE WHEN :sortValue = 2 THEN CAST(limitedMarkets.current_price AS DECIMAL) END ASC,
            CASE WHEN :sortValue = 3 THEN CAST(limitedMarkets.current_price AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 4 THEN CAST(limitedMarkets.price_change_percentage_7d AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 5 THEN CAST(limitedMarkets.price_change_percentage_7d AS DECIMAL) END ASC,
            CASE WHEN :sortValue = 6 THEN CAST(limitedMarkets.price_change_percentage_24h AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 7 THEN CAST(limitedMarkets.price_change_percentage_24h AS DECIMAL) END ASC
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
        ) AS limitedFavoredMarkets
        ORDER BY 
            CASE WHEN :sortValue = 0 THEN CAST(limitedFavoredMarkets.market_cap_rank AS INTEGER) END ASC,
            CASE WHEN :sortValue = 1 THEN CAST(limitedFavoredMarkets.market_cap_rank AS INTEGER) END DESC,
            CASE WHEN :sortValue = 2 THEN CAST(limitedFavoredMarkets.current_price AS DECIMAL) END ASC,
            CASE WHEN :sortValue = 3 THEN CAST(limitedFavoredMarkets.current_price AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 4 THEN CAST(limitedFavoredMarkets.price_change_percentage_7d AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 5 THEN CAST(limitedFavoredMarkets.price_change_percentage_7d AS DECIMAL) END ASC,
            CASE WHEN :sortValue = 6 THEN CAST(limitedFavoredMarkets.price_change_percentage_24h AS DECIMAL) END DESC,
            CASE WHEN :sortValue = 7 THEN CAST(limitedFavoredMarkets.price_change_percentage_24h AS DECIMAL) END ASC
        """
    )
    fun getFavoredWeb3Markets(sortValue: Int): PagingSource<Int, MarketItem>

    @Query("SELECT * FROM markets WHERE coin_id = :coinId")
    fun findMarketById(coinId: String): Market?

    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id LEFT JOIN market_coins mc ON mc.coin_id = m.coin_id WHERE mc.asset_id = :assetId")
    suspend fun findMarketItemByAssetId(assetId: String): MarketItem?

    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id WHERE m.coin_id = :coinId")
    suspend fun findMarketItemByCoinId(coinId: String): MarketItem?

    @Query("SELECT m.* FROM markets m ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, CAST(m.market_cap AS INTEGER) ASC")
    fun coinItems(): LiveData<List<CoinItem>>

    @Query("SELECT m.* FROM markets m WHERE coin_id = :coinId")
    suspend fun simpleCoinItem(coinId: String): CoinItem?

    @Query("SELECT m.* FROM markets m LEFT JOIN market_coins mc on mc.coin_id = m.coin_id WHERE mc.asset_id = :assetId")
    suspend fun simpleCoinItemByAssetId(assetId: String): CoinItem?

    @Query("DELETE FROM markets WHERE coin_id = :coinId")
    suspend fun deleteByCoinId(coinId: String)
}