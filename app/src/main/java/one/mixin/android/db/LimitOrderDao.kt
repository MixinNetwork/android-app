package one.mixin.android.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomSQLiteQuery
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.LimitOrder

@Dao
interface LimitOrderDao : BaseDao<LimitOrder> {

    @Query("SELECT * FROM limit_orders ORDER BY created_at DESC")
    fun all(): Flow<List<LimitOrder>>

    @Query("SELECT * FROM limit_orders WHERE state = :state ORDER BY created_at DESC")
    fun byState(state: String): Flow<List<LimitOrder>>

    @RawQuery(observedEntities = [LimitOrder::class])
    fun paged(query: RoomSQLiteQuery): DataSource.Factory<Int, LimitOrder>
}
