package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.GlobalMarket

@Dao
interface GlobalMarketDao : BaseDao<GlobalMarket> {
    @Query("SELECT * FROM global_market")
    fun getGlobalWeb3Market(): LiveData<GlobalMarket?>
}