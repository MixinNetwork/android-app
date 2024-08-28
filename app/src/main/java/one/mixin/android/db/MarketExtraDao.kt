package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.MarketExtra

@Dao
interface MarketExtraDao : BaseDao<MarketExtra> {
    @Query("UPDATE markets_extra SET is_favored = :isFavored WHERE coin_id = :coinId")
    fun update(coinId: String, isFavored: Boolean)
}
