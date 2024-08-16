package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.Market

@Dao
interface MarketDao : BaseDao<Market> {
    @Query("SELECT * FROM markets WHERE asset_id=:assetId")
    fun marketById(assetId:String):LiveData<Market?>
}