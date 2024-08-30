package one.mixin.android.db

import androidx.lifecycle.LiveData
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

    @Query("SELECT m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, CAST(m.market_cap AS INTEGER) DESC LIMIT :limit")
    fun getWeb3Markets(limit:Int): LiveData<List<MarketItem>>

    @Query("SELECT  m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id WHERE mf.is_favored = 1 ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, CAST(m.market_cap AS INTEGER) DESC  LIMIT :limit")
    fun getFavoredWeb3Markets(limit:Int): LiveData<List<MarketItem>>
}