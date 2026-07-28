package one.mixin.android.db.perps

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.api.response.perps.PerpsRank
import one.mixin.android.db.BaseDao

@Dao
interface PerpsRankDao : BaseDao<PerpsRank> {
    @Query("DELETE FROM ranks")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(ranks: List<PerpsRank>) {
        deleteAll()
        if (ranks.isNotEmpty()) {
            insertListSuspend(ranks)
        }
    }
}
