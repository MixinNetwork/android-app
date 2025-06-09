package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.MembershipOrder

@Dao
interface MembershipOrderDao : BaseDao<MembershipOrder> {

    @Query("SELECT * FROM membership_orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<MembershipOrder>

    @Query("SELECT * FROM membership_orders WHERE status = 'initial' ORDER BY created_at DESC LIMIT 1")
    fun getLatestPendingOrderFlow(): Flow<MembershipOrder?>

    @Query("SELECT * FROM membership_orders ORDER BY created_at DESC")
    fun getAllOrdersFlow(): Flow<List<MembershipOrder>>

    @Query("SELECT * FROM membership_orders WHERE order_id = :orderId")
    fun getOrdersFlow(orderId: String): Flow<MembershipOrder?>

}
