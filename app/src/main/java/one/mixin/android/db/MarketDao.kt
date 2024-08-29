package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem

@Dao
interface MarketDao : BaseDao<Market> {
    @Query("SELECT * FROM markets WHERE asset_id = :assetId")
    fun marketById(assetId: String): LiveData<Market?>

    @Query("SELECT m.*,  mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, m.total_volume DESC")
    fun getWeb3Markets(): LiveData<List<MarketItem>>

    @Query("SELECT  m.*, mf.is_favored FROM markets m LEFT JOIN market_favored mf on mf.coin_id = m.coin_id WHERE mf.is_favored = 1 ORDER BY CAST(m.market_cap_rank AS INTEGER) ASC, m.total_volume DESC")
    fun getFavoredWeb3Markets(): LiveData<List<MarketItem>>

    @Query("SELECT * FROM markets WHERE coin_id = :coinId")
    fun marketByCoinId(coinId: String): Market?
}