package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import one.mixin.android.vo.market.MarketCapRank

@Dao
interface MarketCapRankDao : BaseDao<MarketCapRank> {
    @Query("DELETE FROM market_cap_ranks")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(list: List<MarketCapRank>) {
        deleteAll()
        if (list.isNotEmpty()) {
            insertListSuspend(list)
        }
    }
}
