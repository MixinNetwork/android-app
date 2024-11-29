package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCapRank

@Dao
interface MarketCapRankDao : BaseDao<MarketCapRank> {
    @Query("DELETE FROM market_cap_ranks")
    fun deleteAll()

    @Transaction
    fun deleteAndInsertList(list: List<MarketCapRank>) {
        deleteAll()
        insertList(list)
    }
}