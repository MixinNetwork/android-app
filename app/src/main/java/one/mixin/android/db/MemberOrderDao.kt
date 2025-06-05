package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.vo.MemberOrderStatus

@Dao
interface MemberOrderDao : BaseDao<MemberOrder> {

    @Query("SELECT * FROM member_orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<MemberOrder>

    @Query("SELECT * FROM member_orders WHERE status = 'initial' ORDER BY created_at DESC LIMIT 1")
    fun getLatestPendingOrderFlow(): Flow<MemberOrder?>

    @Query("SELECT * FROM member_orders ORDER BY created_at DESC")
    fun getAllOrdersFlow(): Flow<List<MemberOrder>>
}
