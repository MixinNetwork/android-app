package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.PriceInfo

@Dao
interface PriceInfoDao : BaseDao<PriceInfo> {
    @Query("SELECT * FROM price_info WHERE asset_id=:assetId")
    fun priceInfoById(assetId:String):LiveData<PriceInfo?>
}