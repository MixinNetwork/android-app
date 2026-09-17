package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.vo.market.HistoryPrice

@Dao
interface HistoryPriceDao : BaseDao<HistoryPrice> {
    @Query("SELECT hp.* FROM history_prices hp LEFT JOIN market_coins mc on mc.coin_id = hp.coin_id WHERE mc.asset_id = :assetId AND type = '1D'")
    fun historyPriceById(assetId: String): LiveData<HistoryPrice?>
}
