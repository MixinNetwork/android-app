package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.MemberOrder

@Dao
interface MemberOrderDao : BaseDao<MemberOrder> {

    @Query("SELECT * FROM member_orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<MemberOrder>
}
