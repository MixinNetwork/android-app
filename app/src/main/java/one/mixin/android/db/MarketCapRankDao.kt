package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import one.mixin.android.vo.market.MarketCapRank

@Dao
interface MarketCapRankDao : BaseDao<MarketCapRank> {
    @Query("DELETE FROM market_cap_ranks")
    fun deleteAll()

    @Transaction
    fun insertAll(list: List<MarketCapRank>) {
        deleteAll()
        insertList(list)
    }
}
