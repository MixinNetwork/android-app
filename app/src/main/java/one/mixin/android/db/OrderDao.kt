package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.vo.route.SwapOrder
import one.mixin.android.vo.route.SwapOrderItem

@Dao
interface OrderDao : BaseDao<SwapOrder> {

    @Query(
        """
        SELECT o.*, t.icon_url as asset_icon_url, rt.icon_url as receive_asset_icon_url, rt.symbol as receive_asset_symbol, t.symbol as asset_symbol,  pc.name AS pay_chain_name, rc.name AS receive_chain_name FROM swap_orders o
        LEFT JOIN tokens t ON o.pay_asset_id = t.asset_id
        LEFT JOIN tokens rt ON o.receive_asset_id = rt.asset_id
        LEFT JOIN chains pc ON t.chain_id = pc.chain_id
        LEFT JOIN chains rc ON rt.chain_id = rc.chain_id
        ORDER BY o.created_at DESC
"""
    )
    fun orders(): Flow<List<SwapOrderItem>>

    @Query(
        """
        SELECT o.*, t.icon_url as asset_icon_url, rt.icon_url as receive_asset_icon_url, rt.symbol as receive_asset_symbol, t.symbol as asset_symbol, rt.chain_id as receive_chain_id, t.chain_id as pay_chain_id,  pc.name AS pay_chain_name, rc.name AS receive_chain_name
        FROM swap_orders o
        LEFT JOIN tokens t ON o.pay_asset_id = t.asset_id
        LEFT JOIN tokens rt ON o.receive_asset_id = rt.asset_id
        LEFT JOIN chains pc ON t.chain_id = pc.chain_id
        LEFT JOIN chains rc ON rt.chain_id = rc.chain_id
        WHERE o.order_id = :orderId
    """
    )
    fun getOrderById(orderId: String): Flow<SwapOrderItem?>

    @Query("SELECT * FROM swap_orders WHERE state = 'pending'")
    suspend fun getPendingOrders(): List<SwapOrder>

    @Query(
        """
        SELECT created_at FROM swap_orders ORDER BY created_at DESC LIMIT 1
    """
    )
    suspend fun lastOrderCreatedAt(): String?
}
