package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem

@Dao
interface MarketDao : BaseDao<Market> {
    @Query("SELECT * FROM markets WHERE asset_id=:assetId")
    fun marketById(assetId:String):LiveData<Market?>

    @Query("SELECT m.* FROM markets m ORDER BY m.total_volume DESC")
    fun getWeb3Markets():LiveData<List<MarketItem>>

    @Query("SELECT m.* FROM markets m LEFT JOIN markets_extra me on me.coin_id == m.coin_id ORDER BY m.total_volume DESC")
    fun getFavoredWeb3Markets():LiveData<List<MarketItem>>
}